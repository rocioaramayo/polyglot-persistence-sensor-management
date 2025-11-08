package ui.components;

@FunctionalInterface
public interface TerminalSessionListener {
    void onSessionFinished(long sessionMillis);
}
