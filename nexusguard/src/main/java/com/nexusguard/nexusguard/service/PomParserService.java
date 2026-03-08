package com.nexusguard.nexusguard.service;

import com.nexusguard.nexusguard.dto.DependencyDTO;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.List;

@Service
public class PomParserService {

    public List<DependencyDTO> parsePom(
                                MultipartFile file) 
                                throws Exception {

        List<DependencyDTO> dependencies = new ArrayList<>();

        // Read the uploaded pom.xml file
        SAXReader reader = new SAXReader();
        Document document = reader.read(
                                file.getInputStream());

        // Get root element
        Element root = document.getRootElement();

        // Find dependencies section
        Element depsElement = root.element("dependencies");

        if (depsElement == null) {
            throw new Exception(
                "No dependencies found in pom.xml");
        }

        // Loop through each dependency
        for (Element dep : 
             depsElement.elements("dependency")) {

            String groupId = 
                dep.elementText("groupId");
            String artifactId = 
                dep.elementText("artifactId");
            String version = 
                dep.elementText("version");

            // Only add if version is present
            // Some dependencies inherit version
            // from parent — skip those for now
            if (version != null 
                && !version.startsWith("$")) {
                dependencies.add(
                    new DependencyDTO(
                        groupId, 
                        artifactId, 
                        version)
                );
            }
        }

        return dependencies;
    }
}