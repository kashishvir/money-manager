package com.BuildMoneyManager.MoneyManager.service;

import com.BuildMoneyManager.MoneyManager.dto.AiRequestDTO;
import com.BuildMoneyManager.MoneyManager.dto.AiResponseDTO;
import com.BuildMoneyManager.MoneyManager.dto.ExpenseDTO;
import com.BuildMoneyManager.MoneyManager.dto.IncomeDTO;
import com.BuildMoneyManager.MoneyManager.entity.ProfileEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiAdvisorService {

    private final IncomeService incomeService;
    private final ExpenseService expenseService;
    private final ProfileService profileService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key:}")
    private String envGeminiKey;

    public AiResponseDTO getSavingInsights(AiRequestDTO requestDTO) {
        try {
            // 1. Resolve Gemini API Key (Backend env variable takes precedence, fallback to UI dynamic input)
            String apiKey = (envGeminiKey != null && !envGeminiKey.trim().isEmpty())
                    ? envGeminiKey
                    : requestDTO.getGeminiKey();

            if (apiKey == null || apiKey.trim().isEmpty()) {
                return AiResponseDTO.builder()
                        .success(false)
                        .message("Gemini API Key is missing! Please configure the key in the settings sidebar.")
                        .build();
            }

            // 2. Query Relational Database for Structured context (Vectorless RAG)
            ProfileEntity profile = profileService.getCurrentProfile();
            List<IncomeDTO> monthlyIncomes = incomeService.getCurrentMonthIncomesForCurrentUser();
            List<ExpenseDTO> monthlyExpenses = expenseService.getCurrentMonthExpensesForCurrentUser();

            BigDecimal totalIncome = monthlyIncomes.stream()
                    .map(IncomeDTO::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalExpense = monthlyExpenses.stream()
                    .map(ExpenseDTO::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal netSavings = totalIncome.subtract(totalExpense);

            // Group transactions by category name
            Map<String, BigDecimal> expenseByCategory = monthlyExpenses.stream()
                    .collect(Collectors.groupingBy(
                            ExpenseDTO::getCategoryName,
                            Collectors.reducing(BigDecimal.ZERO, ExpenseDTO::getAmount, BigDecimal::add)
                    ));

            Map<String, BigDecimal> incomeByCategory = monthlyIncomes.stream()
                    .collect(Collectors.groupingBy(
                            IncomeDTO::getCategoryName,
                            Collectors.reducing(BigDecimal.ZERO, IncomeDTO::getAmount, BigDecimal::add)
                    ));

            // Get top 3 highest expenses
            List<ExpenseDTO> topExpenses = monthlyExpenses.stream()
                    .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                    .limit(3)
                    .collect(Collectors.toList());

            // 3. Construct Context-Injected Prompts
            String currentMonthName = LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            int currentYear = LocalDate.now().getYear();

            StringBuilder dataReport = new StringBuilder();
            dataReport.append("User Name: ").append(profile.getFullName()).append("\n");
            dataReport.append("Active Report Period: ").append(currentMonthName).append(" ").append(currentYear).append("\n\n");
            
            dataReport.append("### FINANCIAL ACCOUNT SNAPSHOT\n");
            dataReport.append("- **Total Income:** \u20B9").append(totalIncome).append("\n");
            dataReport.append("- **Total Expense:** \u20B9").append(totalExpense).append("\n");
            dataReport.append("- **Net Monthly Savings:** \u20B9").append(netSavings).append("\n\n");

            dataReport.append("### INCOME BY CATEGORIES\n");
            if (incomeByCategory.isEmpty()) {
                dataReport.append("No active earnings logged yet.\n");
            } else {
                incomeByCategory.forEach((cat, amt) -> dataReport.append("- **").append(cat).append(":** \u20B9").append(amt).append("\n"));
            }
            dataReport.append("\n");

            dataReport.append("### EXPENSES BY CATEGORIES\n");
            if (expenseByCategory.isEmpty()) {
                dataReport.append("No active spending logged yet.\n");
            } else {
                expenseByCategory.forEach((cat, amt) -> dataReport.append("- **").append(cat).append(":** \u20B9").append(amt).append("\n"));
            }
            dataReport.append("\n");

            dataReport.append("### TOP 3 HIGHEST SPENDING ITEMS\n");
            if (topExpenses.isEmpty()) {
                dataReport.append("No transactions available.\n");
            } else {
                for (int i = 0; i < topExpenses.size(); i++) {
                    ExpenseDTO exp = topExpenses.get(i);
                    dataReport.append(i + 1).append(". **").append(exp.getName()).append("** (Category: ").append(exp.getCategoryName()).append(") \u2014 \u20B9").append(exp.getAmount()).append("\n");
                }
            }

            // Create instructions for Gemini
            String systemInstructions = "You are 'SmartSave AI', a premium certified personal wealth coach and smart budgeting strategist.\n" +
                    "Analyze the user's structured current month financial data report below, identify key trends, and provide helpful advice.\n" +
                    "Focus on these areas:\n" +
                    "1. A brief overview audit summarizing their monthly cash flow.\n" +
                    "2. 3 highly actionable, highly customized recommendations on how they can save money (referencing their specific categories or top transactions where relevant).\n" +
                    "3. Calculate a clear monthly saving projection (e.g. 'If you reduce spending on Category X by 15%, you'll accumulate ₹Y annually!').\n" +
                    "Format your entire response using beautifully structured Markdown. Use checklists, bullet points, and high-contrast tables. Do not mention standard vector RAG databases or key-value setups.\n\n" +
                    "USER DATA REPORT:\n" + dataReport.toString();

            String finalPrompt = systemInstructions;
            if (requestDTO.getPrompt() != null && !requestDTO.getPrompt().trim().isEmpty()) {
                finalPrompt += "\n\nUSER QUESTION / BUDGER TARGET:\n" + requestDTO.getPrompt();
            }

            // 4. Invoke Gemini REST API
            HttpClient client = HttpClient.newHttpClient();
            
            // Build the JSON payload cleanly
            Map<String, Object> textObj = Map.of("text", finalPrompt);
            Map<String, Object> partObj = Map.of("parts", List.of(textObj));
            Map<String, Object> contentObj = Map.of("contents", List.of(partObj));
            String requestBody = objectMapper.writeValueAsString(contentObj);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + apiKey.trim()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode rootNode = objectMapper.readTree(response.body());
                String aiText = rootNode.path("candidates")
                        .path(0)
                        .path("content")
                        .path("parts")
                        .path(0)
                        .path("text")
                        .asText();

                return AiResponseDTO.builder()
                        .response(aiText)
                        .success(true)
                        .message("Insights retrieved successfully!")
                        .build();
            } else {
                return AiResponseDTO.builder()
                        .success(false)
                        .message("Google Gemini API returned error (HTTP " + response.statusCode() + "): " + response.body())
                        .build();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return AiResponseDTO.builder()
                    .success(false)
                    .message("Failed to retrieve insights due to internal system exception: " + e.getMessage())
                    .build();
        }
    }
}
