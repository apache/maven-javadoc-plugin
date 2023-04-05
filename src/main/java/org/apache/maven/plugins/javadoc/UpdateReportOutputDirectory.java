package org.apache.maven.plugins.javadoc;

import java.io.File;

// Refactor: Extract class: Class TestJavadocReport and JavadocReport have same method updateReportOutputDirectory
//           So commonly extracted
public class UpdateReportOutputDirectory {
    File reportOutputDirectory;
    public void updateReportOutputDirectory(File reportOutputDirectory, File thisReportOutputDirectory, String destDir) {
        this.reportOutputDirectory=reportOutputDirectory;
        if (reportOutputDirectory != null
                && destDir != null
                && !reportOutputDirectory.getAbsolutePath().endsWith(destDir)) {
            this.reportOutputDirectory = new File(reportOutputDirectory, destDir);
        } else {
            this.reportOutputDirectory = reportOutputDirectory;
        }
    }
}
