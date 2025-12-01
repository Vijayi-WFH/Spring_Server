package com.tse.core_application.dto;

import com.tse.core_application.custom.model.Edge;
import com.tse.core_application.custom.model.Node;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DependencyGraph {
    private List<Node> nodes;
    private List<Edge> edges;
}

