public class test_json {
    public static void main(String[] args) {
        String out = com.mjga.util.JsonParser.buildChatRequest("ark-code-latest", "hello");
        System.out.println(out);
    }
}
