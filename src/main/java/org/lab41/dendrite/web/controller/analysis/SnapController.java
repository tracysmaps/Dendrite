package org.lab41.dendrite.web.controller.analysis;

import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.MetaGraphTx;
import org.lab41.dendrite.metagraph.models.GraphMetadata;
import org.lab41.dendrite.metagraph.models.JobMetadata;
import org.lab41.dendrite.metagraph.models.ProjectMetadata;
import org.lab41.dendrite.services.MetaGraphService;
import org.lab41.dendrite.services.analysis.SnapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.HashMap;
import java.util.Map;

@Controller
public class SnapController {

    @Autowired
    MetaGraphService metaGraphService;

    @Autowired
    SnapService snapService;

    @RequestMapping(value = "/api/graphs/{graphId}/analysis/snap/{algorithm}", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> startJob(@PathVariable String graphId, @PathVariable String algorithm) throws Exception {

        Map<String, Object> response = new HashMap<>();

        DendriteGraph graph = metaGraphService.getGraph(graphId);
        if (graph == null) {
            response.put("status", "error");
            response.put("msg", "missing graph metadata '" + graphId + "'");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        MetaGraphTx tx = metaGraphService.newTransaction();

        GraphMetadata graphMetadata = tx.getGraph(graphId);
        if (graphMetadata == null) {
            response.put("status", "error");
            response.put("msg", "missing graph metadata '" + graphId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        ProjectMetadata projectMetadata = graphMetadata.getProject();
        if (projectMetadata == null) {
            response.put("status", "error");
            response.put("msg", "missing project metadata for graph '" + graphId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        JobMetadata jobMetadata = tx.createJob(projectMetadata);

        response.put("status", "ok");
        response.put("msg", "job submitted");
        response.put("jobId", jobMetadata.getId());

        tx.commit();

        if (!algorithm.equals("centrality")) {
            response.put("status", "error");
            response.put("msg", algorithm + " not implemented");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        // We can't pass the values directly because they'll live in a separate thread.
        snapService.snapAlgorithm(graph, algorithm, jobMetadata.getId());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
