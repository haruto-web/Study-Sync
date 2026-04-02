package com.example.studysync_project.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

import java.util.concurrent.TimeUnit;

public class GeminiApiClient {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/";
    private static final String API_KEY = com.example.studysync_project.BuildConfig.GEMINI_API_KEY;
    private static GeminiService service;

        /**
         * Model fallback order: when the first model is rate-limited or returns transient 5xx errors,
         * the client will retry with the next model.
         */
        private static final String[] MODEL_FAILOVER_ORDER = new String[]{
                "gemini-2.5-flash",
                "gemini-2.0-flash"
        };

            /**
             * Some environments/keys only expose stable v1 endpoints, others use v1beta.
             * We'll attempt both when we hit 404/429/5xx.
             */
            private static final String[] API_VERSION_FAILOVER_ORDER = new String[]{
                    "v1beta"
            };

    private static GeminiService getService() {
        if (service == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new ModelFailoverInterceptor(API_VERSION_FAILOVER_ORDER, MODEL_FAILOVER_ORDER))
                    .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
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

            /**
             * Generates a short personalized study module from quiz performance and current learner interest.
             */
            public static Call<JsonObject> generatePersonalizedModule(
                String subject,
                int score,
                int total,
                String wrongTopics,
                String interestTopic
            ) {
            int percent = total > 0 ? (score * 100) / total : 0;
            String safeSubject = subject != null && !subject.trim().isEmpty() ? subject.trim() : "General";
            String safeInterest = interestTopic != null && !interestTopic.trim().isEmpty()
                ? interestTopic.trim() : safeSubject;

            StringBuilder prompt = new StringBuilder();
            prompt.append("You are an educational tutor. Create a personalized study module for a learner.\n")
                .append("Quiz subject: ").append(safeSubject).append("\n")
                .append("Score: ").append(score).append("/").append(total).append(" (").append(percent).append("%)\n")
                .append("Current topic interest: ").append(safeInterest).append("\n");

            if (wrongTopics != null && !wrongTopics.trim().isEmpty()) {
                prompt.append("Questions answered incorrectly:\n")
                    .append(wrongTopics.trim())
                    .append("\n");
            }

            prompt.append("Write a practical, student-friendly module between 450 and 650 words with this structure:\n")
                .append("1) Title\n")
                .append("2) Why this matters\n")
                .append("3) Core concepts explained simply\n")
                .append("4) Common mistakes and how to avoid them\n")
                .append("5) A short practice checklist\n")
                .append("Focus strongly on the current topic interest while also addressing weak areas shown by quiz mistakes.\n")
                .append("Return plain text only. Do not return JSON, markdown code fences, or extra commentary.");

            return getService().generateContent(API_KEY, buildRequestBody(prompt.toString()));
            }

    private interface GeminiService {
        @POST("v1beta/models/gemini-1.5-flash:generateContent")
        Call<JsonObject> generateContent(
                @Query("key") String apiKey,
                @Body JsonObject body
        );
    }

    /**
     * Retries the same request against alternate models when the API returns transient failures.
     * This helps when a specific model is temporarily rate-limited or overloaded.
     */
    private static final class ModelFailoverInterceptor implements okhttp3.Interceptor {
        private static final int[] RETRY_STATUS_CODES = new int[]{404, 429, 500, 502, 503, 504};
        private final String[] apiVersions;
        private final String[] models;

        private ModelFailoverInterceptor(String[] apiVersions, String[] models) {
            this.apiVersions = apiVersions != null ? apiVersions : new String[0];
            this.models = models != null ? models : new String[0];
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();

            // If the URL doesn't match the expected model path, just proceed.
            String originalUrl = originalRequest.url().toString();

            String matchedVersion = null;
            int modelsIndex = -1;
            for (String v : apiVersions) {
                String segment = "/" + v + "/models/";
                int idx = originalUrl.indexOf(segment);
                if (idx >= 0) {
                    matchedVersion = v;
                    modelsIndex = idx;
                    break;
                }
            }

            if (matchedVersion == null || modelsIndex < 0 || models.length == 0) {
                return chain.proceed(originalRequest);
            }

            String modelsSegment = "/" + matchedVersion + "/models/";
            int modelStart = modelsIndex + modelsSegment.length();
            int modelEnd = originalUrl.indexOf(":generateContent", modelStart);
            if (modelStart < 0 || modelEnd < 0 || modelEnd <= modelStart) {
                return chain.proceed(originalRequest);
            }

            // Try each API version + model in order.
            Response lastResponse = null;
            for (int v = 0; v < apiVersions.length; v++) {
                String apiVersion = apiVersions[v];
                for (int m = 0; m < models.length; m++) {
                    String model = models[m];

                    String attemptUrl = originalUrl;
                    // swap api version segment if needed
                    if (!apiVersion.equals(matchedVersion)) {
                        attemptUrl = attemptUrl.replace(modelsSegment, "/" + apiVersion + "/models/");
                    }

                    // recompute modelStart/modelEnd for the attemptUrl
                    String attemptModelsSegment = "/" + apiVersion + "/models/";
                    int attemptModelsIndex = attemptUrl.indexOf(attemptModelsSegment);
                    int attemptModelStart = attemptModelsIndex >= 0 ? attemptModelsIndex + attemptModelsSegment.length() : -1;
                    int attemptModelEnd = attemptModelStart >= 0 ? attemptUrl.indexOf(":generateContent", attemptModelStart) : -1;
                    if (attemptModelStart < 0 || attemptModelEnd < 0 || attemptModelEnd <= attemptModelStart) {
                        continue;
                    }

                    attemptUrl = attemptUrl.substring(0, attemptModelStart) + model + attemptUrl.substring(attemptModelEnd);
                    Request attemptRequest = originalRequest.newBuilder().url(attemptUrl).build();

                    if (lastResponse != null) {
                        lastResponse.close();
                    }

                    lastResponse = chain.proceed(attemptRequest);

                    boolean lastAttempt = (v == apiVersions.length - 1) && (m == models.length - 1);
                    if (!shouldRetry(lastResponse.code()) || lastAttempt) {
                        return lastResponse;
                    }
                }
            }

            // Should not be reached, but return the last response if it is.
            return lastResponse != null ? lastResponse : chain.proceed(originalRequest);
        }

        private static boolean shouldRetry(int code) {
            for (int retryCode : RETRY_STATUS_CODES) {
                if (retryCode == code) return true;
            }
            return false;
        }
    }
}
