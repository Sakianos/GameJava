package engine.core;

import java.util.List;

public class CommandConfig {
    private String className;
    private List<String> aliases;

    public String getClassName() { return className; }
    public List<String> getAliases() { return aliases; }
}