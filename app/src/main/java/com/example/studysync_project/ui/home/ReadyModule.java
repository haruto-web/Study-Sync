package com.example.studysync_project.ui.home;

import androidx.annotation.NonNull;

public class ReadyModule {
    @NonNull
    public final String id;
    public final String title;
    public final String gradeLevel;
    public final String subject;
    public final String topic;
    public final String description;
    public final String content;

    public ReadyModule(
            @NonNull String id,
            String title,
            String gradeLevel,
            String subject,
            String topic,
            String description,
            String content
    ) {
        this.id = id;
        this.title = title;
        this.gradeLevel = gradeLevel;
        this.subject = subject;
        this.topic = topic;
        this.description = description;
        this.content = content;
    }
}
