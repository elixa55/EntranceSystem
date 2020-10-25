package matchfinger;
public class FingerprintTemplate {
    volatile ImmutableTemplate immutable = ImmutableTemplate.empty;
    
    public FingerprintTemplate() {}
    
    public FingerprintTemplate deserialize(String templateData) {
        TemplateBuilder builder = new TemplateBuilder();
        builder.deserialize(templateData);
        immutable = new ImmutableTemplate(builder);
        return this;
    }
    
}
