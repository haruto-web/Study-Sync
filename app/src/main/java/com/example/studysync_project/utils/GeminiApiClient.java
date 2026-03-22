package com.example.studysync_project.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public class GeminiApiClient {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/";
    private static final String API_KEY = "AIzaSyDBbxB5cc5kZf6E2x6iy8XYvoOynxf_3x4";
    private static GeminiService service;

    private static GeminiService getService() {
        if (service == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();
            service = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(GeminiService.class);
        }
        return service;
    }

    private static JsonObject buildRequestBody(String prompt) {
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", prompt);
        JsonArray parts = new JsonArray();
        parts.add(textPart);
        JsonObject content = new JsonObject();
        content.add("parts", parts);
        JsonArray contents = new JsonArray();
        contents.add(content);
        JsonObject body = new JsonObject();
        body.add("contents", contents);
        return body;
    }

    /**
     * Generates a quiz JSON from module text
     */
    public static Call<JsonObject> generateQuiz(String moduleText, String subject, int questionCount) {
        String prompt = "You are an educational AI. Based on the following study module, generate exactly "
                + questionCount + " multiple choice questions to test a student's knowledge of the topic: "
                + subject + ".\n\n"
                + "Module content:\n" + moduleText + "\n\n"
                + "Return ONLY a valid JSON array with no extra text, in this exact format:\n"
                + "[\n"
                + "  {\n"
                + "    \"question\": \"Question text here\",\n"
                + "    \"optionA\": \"First option\",\n"
                + "    \"optionB\": \"Second option\",\n"
                + "    \"optionC\": \"Third option\",\n"
                + "    \"optionD\": \"Fourth option\",\n"
                + "    \"correctAnswer\": \"A\"\n"
                + "  }\n"
                + "]\n"
                + "The correctAnswer must be exactly A, B, C, or D.";
        return getService().generateContent(API_KEY, buildRequestBody(prompt));
    }

    /**
     * Analyzes quiz score and returns personalized study recommendations
     */
    public static Call<JsonObject> analyzePerformance(String subject, int score, int total, String wrongTopics) {
        int percent = (score * 100) / total;
        String prompt = "A student just completed a quiz on the subject: " + subject + ".\n"
                + "They scored " + score + " out of " + total + " (" + percent + "%).\n"
                + (wrongTopics != null && !wrongTopics.isEmpty()
                ? "They answered these questions incorrectly:\n" + wrongTopics + "\n\n"
                : "\n")
                + "Based on this performance, provide a short, encouraging, and personalized study recommendation. "
                + "Include:\n"
                + "- A brief assessment of their current knowledge level\n"
                + "- 2 to 3 specific areas they should focus on\n"
                + "- A practical study tip they can apply immediately\n"
                + "Keep the response concise, friendly, and under 150 words.";
        return getService().generateContent(API_KEY, buildRequestBody(prompt));
    }

    private interface GeminiService {
        @POST("v1beta/models/gemini-1.5-flash:generateContent")
        Call<JsonObject> generateContent(
                @Query("key") String apiKey,
                @Body JsonObject body
        );
    }
}
