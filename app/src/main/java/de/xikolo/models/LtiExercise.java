package de.xikolo.models;

import com.squareup.moshi.Json;

import de.xikolo.models.base.RealmAdapter;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import moe.banana.jsonapi2.JsonApi;
import moe.banana.jsonapi2.Resource;

public class LtiExercise extends RealmObject {

    @PrimaryKey
    public String id;

    public String instructions;

    public int weight;

    public int allowedAttempts;

    @JsonApi(type = "lti-exercises")
    public static class JsonModel extends Resource implements RealmAdapter<LtiExercise> {

        public String instructions;

        public int weight;

        @Json(name = "allowed_attempts")
        public int allowedAttempts;

        @Override
        public LtiExercise convertToRealmObject() {
            LtiExercise lti = new LtiExercise();
            lti.id = getId();
            lti.instructions = instructions;
            lti.weight = weight;
            lti.allowedAttempts = allowedAttempts;
            return lti;
        }

    }

}
