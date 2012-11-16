package de.schauderhaft.degraph.graph

/**
 * a special graph for gathering and organizing dependencies in a hirachical fashion.
 *
 * Argument is a category which is by default the identity. A category is a function that returns an outer node for any node and the node itself if no out node is available
 */
class Graph(category : AnyRef => AnyRef = (x) => x,
            filter : AnyRef => Boolean = _ => true,
            edgeFilter : ((AnyRef, AnyRef)) => Boolean = _ => true) {

    def topNodes : Set[AnyRef] = _topNodes
    def contentsOf(group : AnyRef) : Set[AnyRef] = _contents.getOrElse(group, Set())
    def connectionsOf(node : AnyRef) : Set[AnyRef] = _edges.getOrElse(node, Set())

    def add(node : AnyRef) {
        if (!filter(node)) return

        val cat = category(node)
        if (cat == node)
            _topNodes = topNodes + node
        else {
            addNodeToCategory(node, cat)
            add(cat)
        }
    }

    def connect(a : AnyRef, b : AnyRef) {
        addEdge(a, b)
        add(a)
        add(b)
    }

    def allNodes : Set[AnyRef] = content(_topNodes)

    private def content(nodes : Set[AnyRef]) : Set[AnyRef] = {
        val ns = nodes.flatMap(contentsOf(_))
        if (ns.isEmpty)
            nodes
        else
            (nodes ++ ns) ++ content(ns)
    }

    private var _topNodes = Set[AnyRef]()
    private var _contents = Map[AnyRef, Set[AnyRef]]()
    private var _edges = Map[AnyRef, Set[AnyRef]]()

    private def addEdge(a : AnyRef, b : AnyRef) {
        if (filter(a) && filter(b) && edgeFilter(a, b))
            _edges += ((a, connectionsOf(a) + b))
    }

    private def addNodeToCategory(node : AnyRef, cat : AnyRef) = {
        _contents += ((cat, contentsOf(cat) + node))
    }
}