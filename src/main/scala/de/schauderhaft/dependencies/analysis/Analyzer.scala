package de.schauderhaft.dependencies.analysis
import java.util.Collections
import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.JavaConversions.mapAsScalaMap
import com.jeantessier.classreader.LoadListenerVisitorAdapter
import com.jeantessier.classreader.TransientClassfileLoader
import com.jeantessier.dependency.ClassNode
import com.jeantessier.dependency.CodeDependencyCollector
import com.jeantessier.dependency.Node
import com.jeantessier.dependency.NodeFactory
import de.schauderhaft.dependencies.graph.Graph
import com.jeantessier.dependency.FeatureNode

object Analyzer {
    def analyze(sourceFolder : String, categorizer : AnyRef => AnyRef, filter : AnyRef => Boolean) : Graph = {

        def getRootClasses = {
            val loader = new TransientClassfileLoader()
            val factory = new NodeFactory()
            val visitor = new CodeDependencyCollector(factory)
            loader.addLoadListener(new LoadListenerVisitorAdapter(visitor))
            loader.load(Collections.singleton(sourceFolder))
            factory.getClasses
        }

        val classes : scala.collection.mutable.Map[String, ClassNode] = getRootClasses

        val g = new Graph(categorizer, filter)

        val featureOutboundClass = (c : ClassNode) => for (
            f <- c.getFeatures();
            od @ (dummy : FeatureNode) <- f.getOutboundDependencies().toTraversable
        ) yield od.getClassNode()
        // different ways to find classes a class depends on.
        val navigations = List(
            (c : ClassNode) => c.getParents().toTraversable, // finds superclasses
            (c : ClassNode) => c.getOutboundDependencies().toTraversable, // finds classes of fields
            featureOutboundClass)

        for ((_, c) <- classes) {
            g.add(c)

            for (
                nav <- navigations;
                n <- nav(c)
            ) g.connect(c, n)
        }
        return g
    }
}