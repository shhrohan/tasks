package com.example.todo.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;

@Component
public class StartupLogger implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupLogger.class);

    private final Environment env;
    private final DataSource dataSource;

    public StartupLogger(Environment env, DataSource dataSource) {
        this.env = env;
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        log.info("==========================================================================================");
        log.info("Active Profile: {}", Arrays.toString(env.getActiveProfiles()));

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            log.info("Database URL:   {}", metaData.getURL());
            log.info("Database User:  {}", metaData.getUserName());
        } catch (SQLException e) {
            log.error("Failed to fetch database connection details", e);
        }

        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        log.info("Memory Stats: Max={}MB, Total={}MB, Free={}MB, Used={}MB",
                maxMemory / (1024 * 1024),
                totalMemory / (1024 * 1024),
                freeMemory / (1024 * 1024),
                usedMemory / (1024 * 1024));
        log.info("Available Processors: {}", runtime.availableProcessors());

        // Display Code Coverage from JaCoCo report
        printCodeCoverage();

        log.info("==========================================================================================");
    }

    /**
     * Reads and displays JaCoCo code coverage from the CSV report.
     * The CSV is generated at: target/site/jacoco/jacoco.csv
     */
    private void printCodeCoverage() {
        Path csvPath = Paths.get("target/site/jacoco/jacoco.csv");

        if (!Files.exists(csvPath)) {
            log.info("------------------------------------------------------------------------------------------");
            log.info("CODE COVERAGE: Not available (run 'mvn test' to generate coverage report)");
            return;
        }

        log.info("------------------------------------------------------------------------------------------");
        log.info("CODE COVERAGE (from JaCoCo):");

        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            // Skip header line
            String header = reader.readLine();
            if (header == null) {
                log.warn("  Coverage report is empty");
                return;
            }

            // JaCoCo CSV columns:
            // GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,BRANCH_MISSED,BRANCH_COVERED,
            // LINE_MISSED,LINE_COVERED,COMPLEXITY_MISSED,COMPLEXITY_COVERED,METHOD_MISSED,METHOD_COVERED

            long totalInstructionMissed = 0;
            long totalInstructionCovered = 0;
            long totalBranchMissed = 0;
            long totalBranchCovered = 0;
            long totalLineMissed = 0;
            long totalLineCovered = 0;

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 9) {
                    totalInstructionMissed += Long.parseLong(parts[3]);
                    totalInstructionCovered += Long.parseLong(parts[4]);
                    totalBranchMissed += Long.parseLong(parts[5]);
                    totalBranchCovered += Long.parseLong(parts[6]);
                    totalLineMissed += Long.parseLong(parts[7]);
                    totalLineCovered += Long.parseLong(parts[8]);
                }
            }

            long totalInstructions = totalInstructionMissed + totalInstructionCovered;
            long totalBranches = totalBranchMissed + totalBranchCovered;
            long totalLines = totalLineMissed + totalLineCovered;

            double instructionCoverage = totalInstructions > 0
                    ? (totalInstructionCovered * 100.0 / totalInstructions)
                    : 0;
            double branchCoverage = totalBranches > 0
                    ? (totalBranchCovered * 100.0 / totalBranches)
                    : 0;
            double lineCoverage = totalLines > 0
                    ? (totalLineCovered * 100.0 / totalLines)
                    : 0;

            log.info("  Instructions: {}% ({}/{} covered)",
                    String.format("%.2f", instructionCoverage), totalInstructionCovered, totalInstructions);
            log.info("  Branches:     {}% ({}/{} covered)",
                    String.format("%.2f", branchCoverage), totalBranchCovered, totalBranches);
            log.info("  Lines:        {}% ({}/{} covered)",
                    String.format("%.2f", lineCoverage), totalLineCovered, totalLines);

            // Coverage grade
            String grade = getCoverageGrade(lineCoverage);
            log.info("  Grade:        {}", grade);

        } catch (IOException | NumberFormatException e) {
            log.warn("  Failed to read coverage report: {}", e.getMessage());
        }
    }

    private String getCoverageGrade(double coverage) {
        if (coverage >= 90)
            return "A+ (Excellent)";
        if (coverage >= 80)
            return "A  (Very Good)";
        if (coverage >= 70)
            return "B  (Good)";
        if (coverage >= 60)
            return "C  (Acceptable)";
        if (coverage >= 50)
            return "D  (Needs Improvement)";
        return "F  (Poor - Increase Test Coverage!)";
    }
}
