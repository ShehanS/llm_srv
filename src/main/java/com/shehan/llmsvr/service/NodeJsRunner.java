package com.shehan.llmsvr.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

@Component
@Slf4j
public class NodeJsRunner implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        try {
            String userHome = System.getProperty("user.home");

            log.info("Checking Node.js version...");
            ProcessBuilder versionCheck = new ProcessBuilder(
                    "bash", "-c",
                    "source " + userHome + "/.nvm/nvm.sh && nvm use 22 && node --version"
            );
            Process versionProcess = versionCheck.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(versionProcess.getInputStream())
            );
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            versionProcess.waitFor();

            // Check node path
            System.out.println("Checking Node.js path...");
            ProcessBuilder pathCheck = new ProcessBuilder(
                    "bash", "-c",
                    "source " + userHome + "/.nvm/nvm.sh && nvm use 22 && which node"
            );
            Process pathProcess = pathCheck.start();

            BufferedReader pathReader = new BufferedReader(
                    new InputStreamReader(pathProcess.getInputStream())
            );
            String nodePath;
            while ((nodePath = pathReader.readLine()) != null) {
                System.out.println("Node.js path: " + nodePath);
            }
            pathProcess.waitFor();

            // Check npm version
            System.out.println("Checking npm version...");
            ProcessBuilder npmCheck = new ProcessBuilder(
                    "bash", "-c",
                    "source " + userHome + "/.nvm/nvm.sh && nvm use 22 && npm --version"
            );
            Process npmProcess = npmCheck.start();

            BufferedReader npmReader = new BufferedReader(
                    new InputStreamReader(npmProcess.getInputStream())
            );
            String npmVersion;
            while ((npmVersion = npmReader.readLine()) != null) {
                System.out.println("npm version: " + npmVersion);
            }
            npmProcess.waitFor();

            System.out.println("Starting Node.js application...");

            ProcessBuilder pb = new ProcessBuilder(
                    "bash", "-c",
                    "source " + userHome + "/.nvm/nvm.sh && nvm use 22 && node --loader ts-node/esm server.ts"
            );

            pb.directory(new File("../llm_service"));
            pb.inheritIO();
            pb.start();

            System.out.println("Node.js started successfully!");
        } catch (Exception e) {
            System.err.println("Failed to start Node.js: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
