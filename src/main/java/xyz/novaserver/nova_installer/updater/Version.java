package xyz.novaserver.nova_installer.updater;

// From https://gist.github.com/brianguertin/ada4b65c6d1c4f6d3eee3c12b6ce021b
public class Version implements Comparable<Version> {
    private final String name;
    private final int[] numbers;

    public Version(String version) {
        name = version;
        final String[] split = version.replaceAll("[^0-9.]","").split("\\-")[0].split("\\.");
        numbers = new int[split.length];
        for (int i = 0; i < split.length; i++) {
            numbers[i] = Integer.parseInt(split[i]);
        }
    }

    public String getName() {
        return name;
    }

    @Override
    public int compareTo(Version another) {
        final int maxLength = Math.max(numbers.length, another.numbers.length);
        for (int i = 0; i < maxLength; i++) {
            final int left = i < numbers.length ? numbers[i] : 0;
            final int right = i < another.numbers.length ? another.numbers[i] : 0;
            if (left != right) {
                return left < right ? -1 : 1;
            }
        }
        return 0;
    }
}
