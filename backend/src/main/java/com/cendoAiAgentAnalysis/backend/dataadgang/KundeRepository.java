// ID:          D2
// Delsystem:   Dataadgang
// Hvad:        Neo4jRepository<Kunde, String>.
// Se docs/Byggeplan_v0.6.docx

package com.cendoAiAgentAnalysis.backend.dataadgang;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import com.cendoAiAgentAnalysis.backend.model.Kunde; //makes the Kunde class available in this file, so we can use it as a type parameter in the Neo4jRepository interface

// The repository interface for the Kunde entity, extending Neo4jRepository to provide CRUD operations and query methods for Kunde nodes in the Neo4j database.
// It is an interface, so it does not contain any implementation code. Spring Data Neo4j will automatically provide the implementation at runtime. Think contract instead of a regular class
public interface KundeRepository
        //Kunde is the type of object the reposetory works with, and String is the type of the id field in the Kunde class
        extends Neo4jRepository<Kunde, String> {
}