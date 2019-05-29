package org.tyaa.ctfinder.common;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class MySkillsOrganizerNode {

    public Boolean dependenciesSatisfied;

    public String title;
    public String type;
    public String description;
    public String status;
    public Boolean access;

    public Integer left;
    public Integer top;
    public Integer radius;

    public MySkillsOrganizerNode() {
    }

    public MySkillsOrganizerNode(
        Boolean dependenciesSatisfied,
         String title,
         String type,
         String description,
         String status,
         Boolean access,
         Integer left,
         Integer top,
         Integer radius
    ) {
        super();
        this.dependenciesSatisfied = dependenciesSatisfied;
        this.title = title;
        this.type = type;
        this.description = description;
        this.status = status;
        this.access = access;
        this.left = left;
        this.top = top;
        this.radius = radius;
    }

    @Override
    public String toString() {
        return "MySkillsOrganizerNode [dependenciesSatisfied=" + dependenciesSatisfied + ", title=" + title + ", type=" + type + ", description="
            + description + ", status=" + status + ", access=" + access + ", left=" + left + ", top=" + top
            + ", radius=" + radius + "]";
    }
}
