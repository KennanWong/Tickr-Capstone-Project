package tickr.application.serialised.responses.event;

import java.util.List;

public class CategoriesResponse {
    public List<String> categories;

    public CategoriesResponse () {}

    public CategoriesResponse (List<String> categories) {
        this.categories = categories;
    }
}
