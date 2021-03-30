package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities;

import javax.validation.constraints.NotBlank;

public class ProductBody {

    @NotBlank(message = "name cannot be null")
    private final String name;

    @NotBlank(message = "key cannot be null")
    private final String key;

    public ProductBody(String name, String key) {
        this.name = name;
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return "ProductBody{" +
               "name='" + name + '\'' +
               ", key='" + key + '\'' +
               '}';
    }
}
