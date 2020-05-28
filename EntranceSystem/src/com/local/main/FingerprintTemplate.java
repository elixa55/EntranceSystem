package main;
public class FingerprintTemplate {
    volatile ImmutableTemplate immutable = ImmutableTemplate.empty;
    
    public FingerprintTemplate() {
    }
    
    public FingerprintTemplate create(byte[] image) {
        TemplateBuilder builder = new TemplateBuilder();
        builder.extract(image);
        immutable = new ImmutableTemplate(builder);
        return this;
    }
   
    public FingerprintTemplate deserialize(String templateData) {
        TemplateBuilder builder = new TemplateBuilder();
        builder.deserialize(templateData);
        immutable = new ImmutableTemplate(builder);
        return this;
    }
   
    public String serialize() {
        String serialized = "";
        ImmutableTemplate current = immutable;
        TemplateData temp = new TemplateData(current.size, current.minutiae);
        serialized += "width:" + temp.width + " ";
        serialized += "height:" +temp.height + " ";
        for (MinutiaData m : temp.minutiae) {
            serialized += "" + m.x;
            serialized += "#" + m.y;
            serialized += "#" + m.direction;
            serialized += "#" + m.type + ";";
        }
        return serialized;
     }
   
}
