package oathkeeper.runtime.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import oathkeeper.runtime.event.SemanticEvent;
import oathkeeper.runtime.gson.InterfaceAdapter;
import oathkeeper.runtime.template.Template;
import oathkeeper.runtime.template_v1.TemplateV1;

public class GsonUtils {
    public static Gson gsonPrettyPrinter = new GsonBuilder()
            .registerTypeAdapter(SemanticEvent.class, new InterfaceAdapter<SemanticEvent>())
            .registerTypeAdapter(Template.class, new InterfaceAdapter<Template>())
            .registerTypeAdapter(TemplateV1.class, new InterfaceAdapter<TemplateV1>())
            .setPrettyPrinting().create();

}
