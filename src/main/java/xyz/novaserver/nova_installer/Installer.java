package xyz.novaserver.nova_installer;
import com.formdev.flatlaf.FlatDarkLaf;
import net.fabricmc.installer.Main;
import net.fabricmc.installer.util.MetaHandler;
import net.fabricmc.installer.util.Reference;
import net.fabricmc.installer.util.Utils;
import xyz.novaserver.nova_installer.layouts.VerticalLayout;
import org.json.JSONException;
import xyz.novaserver.nova_installer.updater.UpdateMeta;
import xyz.novaserver.nova_installer.updater.Version;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Installer {
    private final String BASE_URL;
    private final Properties INSTALLER_INFO;
    private final List<InstallerMeta.Edition> EDITIONS;
    private final List<String> GAME_VERSIONS;

    private String selectedEditionName;
    private String selectedEditionDisplayName;
    private String selectedVersion;
    private Path customInstallDir;

    private JButton installButton;
    private JComboBox<String> editionDropdown;
    private JComboBox<String> versionDropdown;
    private JButton installDirectoryPicker;
    private JProgressBar progressBar;

    boolean finishedSuccessfulInstall = false;

    public Installer() {
        FlatDarkLaf.setup();
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            System.out.println("Failed to set UI theme!");
            e.printStackTrace();
        }

        Main.LOADER_META = new MetaHandler(Reference.getMetaServerEndpoint("v2/versions/loader"));
        try {
            Main.LOADER_META.load();
        } catch (Exception e) {
            System.out.println("Failed to fetch fabric version info from the server!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "The installer was unable to fetch fabric version info from the server," +
                    "please check your internet connection and try again later.", "Please check your internet connection!", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        INSTALLER_INFO = new Properties();
        try {
            INSTALLER_INFO.load(getClass().getClassLoader().getResourceAsStream("info.properties"));
        } catch (IOException e) {
            System.out.println("Failed to load local installer metadata!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Installer metadata parsing failed, please contact Lui798!" +
                    "\nError: " + e, "Metadata Parsing Failed!", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        BASE_URL = INSTALLER_INFO.getProperty("base-url");

        InstallerMeta installerMeta = new InstallerMeta(BASE_URL + "meta.json");
        try {
            installerMeta.load();
        } catch (IOException e) {
            System.out.println("Failed to fetch installer metadata from the server!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "The installer was unable to fetch metadata from the server, " +
                    "please check your internet connection and try again later.", "Please check your internet connection!", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        } catch (JSONException e) {
            System.out.println("Failed to parse metadata!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Installer metadata parsing failed, please contact Lui798!" +
                    "\nError: " + e, "Metadata Parsing Failed!", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        GAME_VERSIONS = installerMeta.getGameVersions();
        EDITIONS = installerMeta.getEditions();
    }

    public static void main(String[] args) {
        System.out.println("Launching installer...");
        new Installer().start();
    }

    public void start() {
        JFrame frame = new JFrame("Nova Installer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setSize(350,300);
        frame.setLocationRelativeTo(null); // Centers the window
        frame.setIconImage(new ImageIcon(Objects.requireNonNull(Utils.class.getClassLoader().getResource("nova_icon.png"))).getImage());

        JPanel topPanel = new JPanel(new VerticalLayout());

        JPanel editionPanel = new JPanel();
        JLabel editionDropdownLabel = new JLabel("Select Edition:");
        editionDropdownLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        List<String> editionNames = new ArrayList<>();
        List<String> editionDisplayNames = new ArrayList<>();
        for (InstallerMeta.Edition edition : EDITIONS) {
            editionNames.add(edition.name);
            editionDisplayNames.add(edition.displayName);
        }
        String[] editionNameList = editionNames.toArray(new String[0]);
        selectedEditionName = editionNameList[0];
        String[] editionDisplayNameList = editionDisplayNames.toArray(new String[0]);
        selectedEditionDisplayName = editionDisplayNameList[0];
        editionDropdown = new JComboBox<>(editionDisplayNameList);
        editionDropdown.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                selectedEditionName = editionNameList[editionDropdown.getSelectedIndex()];
                selectedEditionDisplayName = (String) e.getItem();
                if (customInstallDir == null) {
                    installDirectoryPicker.setText(getDefaultInstallDir().toFile().getName());
                }

                readyAll();
            }
        });
        editionPanel.add(editionDropdownLabel);
        editionPanel.add(editionDropdown);

        JPanel versionPanel = new JPanel();
        JLabel versionDropdownLabel = new JLabel("Select Game Version:");
        versionDropdownLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        List<String> gameVersions = GAME_VERSIONS.subList(0, GAME_VERSIONS.size()); // Clone the list
        Collections.reverse(gameVersions); // Reverse the order of the list so that the latest version is on top and older versions downward
        String[] gameVersionList = gameVersions.toArray(new String[0]);
        selectedVersion = gameVersionList[0];
        versionDropdown = new JComboBox<>(gameVersionList);
        versionDropdown.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                selectedVersion = (String) e.getItem();

                readyAll();
            }
        });
        versionPanel.add(versionDropdownLabel);
        versionPanel.add(versionDropdown);

        JPanel installDirectoryPanel = new JPanel();
        JLabel installDirectoryPickerLabel = new JLabel("Select Install Directory:");
        installDirectoryPickerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        installDirectoryPicker = new JButton(getDefaultInstallDir().toFile().getName());
        installDirectoryPicker.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setFileHidingEnabled(false);
            int option = fileChooser.showOpenDialog(frame);
            if (option == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                customInstallDir = file.toPath();
                installDirectoryPicker.setText(file.getName());

                readyAll();
            }
        });
        installDirectoryPanel.add(installDirectoryPickerLabel);
        installDirectoryPanel.add(installDirectoryPicker);

        topPanel.add(editionPanel);
        topPanel.add(versionPanel);
        topPanel.add(installDirectoryPanel);

        JPanel bottomPanel = new JPanel();

        progressBar = new JProgressBar();
        progressBar.setValue(0);
        progressBar.setMaximum(100);
        progressBar.setStringPainted(true);

        installButton = new JButton("Install");
        installButton.addActionListener(action -> {
            InstallerMeta.Edition matchedEdition = EDITIONS.stream().filter(edition -> edition.name.equals(selectedEditionName)).findFirst().get();

            if (!matchedEdition.compatibleVersions.contains(selectedVersion)) {
                JOptionPane.showMessageDialog(frame, "The selected edition is not compatible with the chosen game version.",
                        "Incompatible Edition", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (matchedEdition.unstable) {
                int result = JOptionPane.showOptionDialog(frame, "The selected edition is marked as unstable! " +
                                "You may experience crashes or other stability errors while playing.\n\nContinue with installation?",
                        "Unstable Edition", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
                if (result != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            final String loaderName = "fabric-loader";
            try {
                String loaderVersion = Main.LOADER_META.getLatestVersion(false).getVersion();
                boolean success = VanillaLauncherIntegration.installToLauncher(getVanillaGameDir(), getInstallDir(),
                        selectedEditionDisplayName, selectedVersion, loaderName, loaderVersion, VanillaLauncherIntegration.Icon.NOVA);
                if (!success) {
                    System.out.println("Failed to install to launcher, canceling!");
                    return;
                }
            } catch (IOException e) {
                System.out.println("Failed to install version and profile to vanilla launcher!");
                e.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Failed to install to vanilla launcher, please contact Lui798!" +
                        "\nError: " + e, "Failed to install to launcher", JOptionPane.ERROR_MESSAGE);
                return;
            }

            File storageDir = getStorageDirectory().toFile();
            if (!storageDir.exists() || !storageDir.isDirectory()) {
                storageDir.mkdir();
            }

            installButton.setText("Downloading...");
            progressBar.setValue(0);
            setInteractionEnabled(false);

            final String zipName = selectedEditionName + ".zip";
            final String downloadURL = BASE_URL + selectedVersion + "/" + zipName;
            final File saveLocation = getStorageDirectory().resolve(zipName).toFile();
            final Downloader downloader = new Downloader(downloadURL, saveLocation);

            downloader.addPropertyChangeListener(event -> {
                if ("progress".equals(event.getPropertyName())) {
                    progressBar.setValue((Integer) event.getNewValue());
                } else if (event.getNewValue() == SwingWorker.StateValue.DONE) {
                    try {
                        downloader.get();
                    } catch (InterruptedException | ExecutionException e) {
                        System.out.println("Failed to download zip!");
                        e.getCause().printStackTrace();

                        String msg = String.format("An error occurred while attempting to download the required files, " +
                                "please check your internet connection and try again! \nError: %s", e.getCause().toString());
                        JOptionPane.showMessageDialog(frame,
                                msg, "Download Failed!", JOptionPane.ERROR_MESSAGE, null);
                        readyAll();

                        return;
                    }

                    installButton.setText("Download completed!");

                    File installDir = getInstallDir().toFile();
                    if (!installDir.exists() || !installDir.isDirectory()) installDir.mkdirs();

                    File modsFolder = getInstallDir().resolve("mods").toFile();
                    File[] modsFolderContents = modsFolder.listFiles();
                    if (modsFolderContents != null) {
                        boolean isEmpty = modsFolderContents.length == 0;

                        if (modsFolder.exists() && modsFolder.isDirectory() && !isEmpty) {
                            int result = JOptionPane.showConfirmDialog(frame,"An existing mods folder was found in the selected game directory. " +
                                            "Do you want to delete all existing mods before installation to prevent version conflicts?\n\n" +
                                            "(Unless you know exactly what you're doing and know how to solve conflicts, press \"Yes\")", "Mods Folder Detected",
                                    JOptionPane.YES_NO_OPTION,
                                    JOptionPane.QUESTION_MESSAGE);
                            if (result == JOptionPane.YES_OPTION) {
                                deleteDirectory(modsFolder);
                            }
                        }
                    }

                    if (!modsFolder.exists() || !modsFolder.isDirectory()) modsFolder.mkdirs();

                    boolean installSuccess = installFromZip(saveLocation);
                    if (installSuccess) {
                        installButton.setText("Installation succeeded!");
                        finishedSuccessfulInstall = true;
                        editionDropdown.setEnabled(true);
                        versionDropdown.setEnabled(true);
                        installDirectoryPicker.setEnabled(true);
                        JOptionPane.showMessageDialog(frame, "Successfully installed " + selectedEditionDisplayName +
                                        " to your Minecraft launcher! Run this installer again when you want to update the pack.",
                                "Installation Succeeded!", JOptionPane.INFORMATION_MESSAGE);
                        System.exit(0);
                    } else {
                        installButton.setText("Installation failed!");
                        System.out.println("Failed to install to mods folder!");
                        JOptionPane.showMessageDialog(frame, "Failed to install to mods folder, " +
                                "please make sure your game is closed and try again!", "Installation Failed!", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            downloader.execute();
        });

        bottomPanel.add(progressBar);
        bottomPanel.add(installButton);

        frame.getContentPane().add(topPanel, BorderLayout.NORTH);
        frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);
        frame.setVisible(true);

        System.out.println("Launched!");

        // Check for updates and notify the user
        UpdateMeta updateMeta = new UpdateMeta(INSTALLER_INFO.getProperty("updater-api-url"),
                new Version(INSTALLER_INFO.getProperty("version")));
        try {
            updateMeta.load();
            if (!updateMeta.hasLatestVersion()) {
                int result = JOptionPane.showConfirmDialog(frame,"An update for Nova Installer is available: " +
                                updateMeta.getLatestVersion().getName() +
                                "\nWe recommend staying up to date to ensure you have the latest fixes & features!" +
                                "\n\nWould you like to open the download page now?","Nova Installer Update",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE);
                if (result == JOptionPane.YES_OPTION) {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(new URI(updateMeta.getUpdateUrl()));
                        System.exit(0);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Failed to load update metadata!");
            e.printStackTrace();
        } catch (URISyntaxException e) {
            System.out.println("Failed to open update url!");
            e.printStackTrace();
        }
    }

    // Works up to 2GB because of long limitation
    private static class Downloader extends SwingWorker<Void, Void> {
        private final String url;
        private final File file;

        public Downloader(String url, File file) {
            this.url = url;
            this.file = file;
        }

        @Override
        protected Void doInBackground() throws Exception {
            URL url = new URL(this.url);
            HttpsURLConnection connection = (HttpsURLConnection) url
                    .openConnection();
            long filesize = connection.getContentLengthLong();
            if (filesize == -1) {
                throw new Exception("Content length must not be -1 (unknown)!");
            }
            long totalDataRead = 0;
            try (java.io.BufferedInputStream in = new java.io.BufferedInputStream(
                    connection.getInputStream())) {
                java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
                try (java.io.BufferedOutputStream bout = new BufferedOutputStream(
                        fos, 1024)) {
                    byte[] data = new byte[1024];
                    int i;
                    while ((i = in.read(data, 0, 1024)) >= 0) {
                        totalDataRead = totalDataRead + i;
                        bout.write(data, 0, i);
                        int percent = (int) ((totalDataRead * 100) / filesize);
                        setProgress(percent);
                    }
                }
            }
            return null;
        }
    }

    public boolean installFromZip(File zip) {
        try {
            int BUFFER_SIZE = 2048; // Buffer Size

            ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zip));
            ZipEntry entry = zipIn.getNextEntry();
            // iterates over entries in the zip file
            while (entry != null) {
                String entryName = entry.getName();

                File filePath = getInstallDir().resolve(entryName).toFile();
                if (!entry.isDirectory()) {
                    // if the entry is a file, extracts it
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
                    byte[] bytesIn = new byte[BUFFER_SIZE];
                    int read = 0;
                    while ((read = zipIn.read(bytesIn)) != -1) {
                        bos.write(bytesIn, 0, read);
                    }
                    bos.close();
                } else {
                    // if the entry is a directory, make the directory
                    filePath.mkdir();
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
            zipIn.close();
            return true;
        } catch (IOException e) {
            e.getCause().printStackTrace();
            return false;
        }
    }

    public Path getStorageDirectory() {
        return getAppDataDirectory().resolve(getStorageDirectoryName());
    }

    public Path getInstallDir() {
        return customInstallDir != null ? customInstallDir : getDefaultInstallDir();
    }

    public Path getAppDataDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win"))
            return new File(System.getenv("APPDATA")).toPath();
        else if (os.contains("mac"))
            return new File(System.getProperty("user.home") + "/Library/Application Support").toPath();
        else if (os.contains("nux"))
            return new File(System.getProperty("user.home")).toPath();
        else
            return new File(System.getProperty("user.dir")).toPath();
    }

    public String getStorageDirectoryName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac"))
            return "nova-installer";
        else
            return ".nova-installer";
    }

    public Path getDefaultInstallDir() {
        String os = System.getProperty("os.name").toLowerCase();
        Path mcDir;

        if (os.contains("mac"))
            mcDir = getAppDataDirectory().resolve("minecraft");
        else
            mcDir = getAppDataDirectory().resolve(".minecraft");

        return mcDir.resolve("packs").resolve(selectedEditionName);
    }

    public Path getVanillaGameDir() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("mac") ? getAppDataDirectory().resolve("minecraft") : getAppDataDirectory().resolve(".minecraft");
    }

    public boolean deleteDirectory(File dir) {
        File[] allContents = dir.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return dir.delete();
    }

    public void setInteractionEnabled(boolean enabled) {
        editionDropdown.setEnabled(enabled);
        versionDropdown.setEnabled(enabled);
        installDirectoryPicker.setEnabled(enabled);
        installButton.setEnabled(enabled);
    }

    public void readyAll() {
        finishedSuccessfulInstall = false;
        installButton.setText("Install");
        progressBar.setValue(0);
        setInteractionEnabled(true);
    }
}
