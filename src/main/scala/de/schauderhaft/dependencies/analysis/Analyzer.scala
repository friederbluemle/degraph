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
import de.schauderhaft.dependencies.categorizer.InternalClassCategorizer
import de.schauderhaft.dependencies.categorizer.MultiCategorizer
import de.schauderhaft.dependencies.graph.Graph
import com.jeantessier.dependency.FeatureNode

object Analyzer {
    def analyze(sourceFolder : String) : Graph = {
        def debug(text : String, from : Node, to : Node) {
            val prefix = List("de.schauderhaft.dependencies.example", "org.junit")
            def shouldPrint(node : Node) =
                prefix.exists(node.getName.startsWith(_))
            if (shouldPrint(from) && shouldPrint(to))
                println("%s: %s --> %s".format(text, from.getName, to.getName))
        }

        def getRootClasses = {
            val loader = new TransientClassfileLoader()
            val factory = new NodeFactory()
            val visitor = new CodeDependencyCollector(factory)
            loader.addLoadListener(new LoadListenerVisitorAdapter(visitor))
            loader.load(Collections.singleton(sourceFolder))
            factory.getClasses
        }

        val classes : scala.collection.mutable.Map[String, ClassNode] = getRootClasses

        val g = new Graph( InternalClassCategorizer)

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