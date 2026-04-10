package com.example.studysync_project.ui.home;

import androidx.annotation.NonNull;

public class ReadyModule {
    @NonNull
    public final String id;
    public final String title;
    public final String gradeLevel;
    public final String strand; // null for non-SHS
    public final String subject;
    public final String topic;
    public final String description;
    public final String content;
    public final String difficulty; // Beginner / Intermediate / Advanced
    public final String lessons;   // newline-separated lesson titles

    // Full constructor
    public ReadyModule(
            @NonNull String id,
            String title,
            String gradeLevel,
            String strand,
            String subject,
            String topic,
            String description,
            String content,
            String difficulty,
            String lessons
    ) {
        this.id = id;
        this.title = title;
        this.gradeLevel = gradeLevel;
        this.strand = strand;
        this.subject = subject;
        this.topic = topic;
        this.description = description;
        this.content = content;
        this.difficulty = difficulty;
        this.lessons = lessons;
    }

    // Legacy constructor (non-SHS, no difficulty/lessons) — keeps existing catalog entries compiling
    public ReadyModule(
            @NonNull String id,
            String title,
            String gradeLevel,
            String subject,
            String topic,
            String description,
            String content
    ) {
        this(id, title, gradeLevel, null, subject, topic, description, content, "Beginner", null);
    }
}
