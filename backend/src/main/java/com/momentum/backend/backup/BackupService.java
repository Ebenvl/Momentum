package com.momentum.backend.backup;

import com.momentum.backend.config.Config;
import com.momentum.backend.db.ActivityLogRepository;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

// Backs up the whole database once a day with mysqldump, MySQL's own backup tool.
// Each backup is one .sql file in backend/backups, named by date, and the
// last 7 are kept. backups/ is in .gitignore, so they never go to GitHub.
//
// To restore one, in MySQL Workbench as root: File, Open SQL Script, pick the
// file, and run it. It recreates every table with its data.
public class BackupService {

    private static final Path FOLDER = Path.of("backups");
    private static final int KEEP = 7;

    private final ActivityLogRepository activityLogRepository;
    private final String mysqldump;
    private final String databaseUrl;
    private final String user;
    private final String password;

    public BackupService(Config config, ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
        this.mysqldump = findMysqldump(config);
        this.databaseUrl = config.get("db.url");
        this.user = config.get("db.user");
        this.password = config.get("db.password");
    }

    // Runs every hour from App.java, but only backs up if there is no backup for today yet.
    // Never throws, so a failed backup can't stop the schedule.
    public synchronized void backupIfDue() {
        Path file = FOLDER.resolve("momentum-" + LocalDate.now() + ".sql");
        if (Files.exists(file)) {
            return;
        }

        try {
            Files.createDirectories(FOLDER);

            // jdbc:mysql://localhost:3306/momentum gives host localhost, port 3306, database momentum.
            String address = databaseUrl.substring(databaseUrl.indexOf("//") + 2);
            String hostAndPort = address.substring(0, address.indexOf("/"));
            String database = address.substring(address.indexOf("/") + 1).split("\\?")[0];
            String host = hostAndPort.contains(":") ? hostAndPort.split(":")[0] : hostAndPort;
            String port = hostAndPort.contains(":") ? hostAndPort.split(":")[1] : "3306";

            // --single-transaction takes a consistent copy without locking the tables,
            // --no-tablespaces means the app user's limited rights are enough.
            ProcessBuilder builder = new ProcessBuilder(mysqldump,
                    "--host=" + host, "--port=" + port, "--user=" + user,
                    "--single-transaction", "--no-tablespaces",
                    "--result-file=" + file.toAbsolutePath(), database);

            // The password goes in through the environment, not the command, so it
            // doesn't show up in the list of running programs.
            builder.environment().put("MYSQL_PWD", password);
            builder.redirectErrorStream(true);

            Process process = builder.start();
            String output = new String(process.getInputStream().readAllBytes());
            boolean finished = process.waitFor(5, TimeUnit.MINUTES);

            if (!finished || process.exitValue() != 0) {
                Files.deleteIfExists(file);
                fail(finished ? output.trim() : "took longer than 5 minutes");
                return;
            }

            removeOldBackups();
            System.out.println("Database backed up to " + file);
            activityLogRepository.log(null, "backup: " + file.getFileName(), "info");
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    // Keeps only the newest KEEP backups. The names have the date in them,
    // so sorting by name is sorting by date.
    private void removeOldBackups() {
        File[] files = FOLDER.toFile().listFiles((dir, name) -> name.startsWith("momentum-") && name.endsWith(".sql"));
        if (files == null || files.length <= KEEP) {
            return;
        }
        Arrays.sort(files, Comparator.comparing(File::getName));
        for (int i = 0; i < files.length - KEEP; i++) {
            files[i].delete();
        }
    }

    private void fail(String reason) {
        System.out.println("Backup failed: " + reason);
        try {
            activityLogRepository.log(null, "backup_failed: " + reason, "error");
        } catch (Exception logError) {
            System.out.println("Could not write to the activity log either: " + logError.getMessage());
        }
    }

    // Where mysqldump is. backup.mysqldump in config.properties if it's set,
    // otherwise it looks in the usual MySQL install folder on Windows,
    // otherwise it hopes "mysqldump" works on its own.
    private static String findMysqldump(Config config) {
        String fromConfig = config.getOptional("backup.mysqldump");
        if (fromConfig != null) {
            return fromConfig;
        }

        File[] servers = new File("C:/Program Files/MySQL").listFiles((dir, name) -> name.startsWith("MySQL Server"));
        if (servers != null) {
            for (File server : servers) {
                File tool = new File(server, "bin/mysqldump.exe");
                if (tool.exists()) {
                    return tool.getAbsolutePath();
                }
            }
        }
        return "mysqldump";
    }
}
