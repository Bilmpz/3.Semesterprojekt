package com.cendoAiAgentAnalysis.backend.analyse.config;

import com.cendoAiAgentAnalysis.backend.dataadgang.AiAgentRepository;
import com.cendoAiAgentAnalysis.backend.dataadgang.KundeRepository;
import com.cendoAiAgentAnalysis.backend.model.AiAgent;
import com.cendoAiAgentAnalysis.backend.model.Kunde;

import org.springframework.boot.CommandLineRunner; //Allows the class to run code at application startup, so "Run" is called
import org.springframework.stereotype.Component; //Does so springboot can find this class and run it at startup

@Component
public class TestData implements CommandLineRunner {

    private final KundeRepository kundeRepository;
    private final AiAgentRepository aiAgentRepository;

    // Constructor  - injection of the repositories, so we can use them to save and retrieve data from the database
    public TestData(
            KundeRepository kundeRepository,
            AiAgentRepository aiAgentRepository) {
        this.kundeRepository = kundeRepository;
        this.aiAgentRepository = aiAgentRepository;
    }

    @Override
    public void run(String... args) {

        // Create and save test customers
        Kunde kunde1 = new Kunde("1", "Kunde 1: Anna");
        Kunde kunde2 = new Kunde("2", "Kunde 2: Bob");

        // Calls the save method of the kundeRepository to persist the Kunde objects in the database
        kundeRepository.save(kunde1);
        kundeRepository.save(kunde2);

        // Create and save test AI agents
        AiAgent aiAgent1 = new AiAgent("1", "AI Agent 1");
        AiAgent aiAgent2 = new AiAgent("2", "AI Agent 2");

        //Calls the save method of the aiAgentRepository to persist the AiAgent objects in the database
        aiAgentRepository.save(aiAgent1);
        aiAgentRepository.save(aiAgent2);

        // Prints the saved customers to the console
        System.out.println("\nKunder");
        kundeRepository.findAll().forEach(kunde ->
                System.out.println(
                        "ID: " + kunde.getId()
                        + ", Navn: " + kunde.getNavn()
                )
        );

        // Prints the saved AI agents to the console
        System.out.println("\nAI-agenter");
        aiAgentRepository.findAll().forEach(agent ->
                System.out.println(
                        "ID: " + agent.getId()
                        + ", Navn: " + agent.getNavn()
                )
        );

        // Prints the count of customers and AI agents in the database to the console
        System.out.println("\nAntal kunder: " + kundeRepository.count());
        System.out.println("Antal AI-agenter: " + aiAgentRepository.count());
    }
}