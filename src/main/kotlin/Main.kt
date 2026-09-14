import model.Player
import model.Position
import model.Team
import resolver.Resolver
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartFrame
import org.jfree.chart.ChartPanel
import org.jfree.chart.plot.PlotOrientation
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer
import org.jfree.data.category.DefaultCategoryDataset
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import parser.CsvParser
import java.awt.Color
import javax.swing.JFrame

fun main(args: Array<String>) {
    val result = CsvParser("src/main/resources/fakePlayersDirty.csv").parseFile();
    val resolver = Resolver(result.first, result.second.values.toMutableList());

    println("getCountWithoutAgency: ${resolver.getCountWithoutAgency()}");
    println("getBestScorerDefender: ${resolver.getBestScorerDefender()}");
    println("getTheExpensiveGermanPlayerPosition: ${resolver.getTheExpensiveGermanPlayerPosition()}");
    println("getTheRudestTeam: ${resolver.getTheRudestTeam()}");

    val positions = resolver.getAverageTransferCostByPosition()
    println("getAverageTransferCostByPosition:");
    for (position in positions) {
        println("${position.key}: ${"%.2f".format(position.value)}")
    }

    println("getMostValuablePlayers: ${resolver.getMostValuablePlayers()}");
    println("getMostPopularAgencyByCountry: ${resolver.getMostPopularAgencyByCountry()}");
    println("getGoalsShareByMedianCost: ${resolver.getGoalsShareByMedianCost()}");

    baseVisualization(resolver.players, resolver.teams)
    advanceVizualization(resolver.players, resolver.teams)
}

fun baseVisualization(players: MutableList<Player>, teams: MutableList<Team>) {
    val dataset = DefaultCategoryDataset()
    val teamsByTransfer = teams.sortedBy{ team ->
        team.players.sumOf { it.transferCost }
    }

    for (team in teamsByTransfer) {
        dataset.addValue(team.players.sumOf { it.transferCost }, team.name, "" )
    }

    val chart = ChartFactory.createBarChart(
        "Топ-10 команд по суммарной трансферной стоимости",
        "Команды",
        "Сумма",
        dataset,
        PlotOrientation.VERTICAL,
        true,
        true,
        false
    )

    val frame = ChartFrame("Мой график", chart)
    frame.setSize(1000, 750)
    frame.isVisible = true
}

fun advanceVizualization(players: MutableList<Player>, teams: MutableList<Team>) {
    val forwards = players
        .filter { it.position == Position.FORWARD }
        .sortedBy { it.transferCost }

    val windowWidth = 2

    val movingAvg = forwards
        .map { it.goals.toDouble() }
        .windowed(windowWidth) { window -> window.average() }

    val xForAvg = forwards
        .map { it.transferCost.toDouble() }
        .windowed(windowWidth) { window -> window.average() }

    val lowYellow = forwards.filter { it.yellowCards <= 3 }
    val midYellow = forwards.filter { it.yellowCards in 4..6 }
    val highYellow = forwards.filter { it.yellowCards >= 7 }

    val dataset = XYSeriesCollection()

    val lowSeries = XYSeries("0–3 карточки")
    lowYellow.forEach { lowSeries.add(it.transferCost, it.goals) }
    dataset.addSeries(lowSeries)

    val midSeries = XYSeries("4–6 карточек")
    midYellow.forEach { midSeries.add(it.transferCost, it.goals) }
    dataset.addSeries(midSeries)

    val highSeries = XYSeries("7+ карточек")
    highYellow.forEach { highSeries.add(it.transferCost, it.goals) }
    dataset.addSeries(highSeries)

    val trendSeries = XYSeries("Тренд (скользящее среднее)")
    xForAvg.zip(movingAvg)
        .forEach { (x, y) -> trendSeries.add(x, y) }
    dataset.addSeries(trendSeries)

    val renderer = XYLineAndShapeRenderer()
    renderer.setSeriesLinesVisible(0, false)
    renderer.setSeriesShapesVisible(0, true)
    renderer.setSeriesLinesVisible(1, false)
    renderer.setSeriesShapesVisible(1, true)
    renderer.setSeriesLinesVisible(2, false)
    renderer.setSeriesShapesVisible(2, true)
    renderer.setSeriesLinesVisible(3, true)
    renderer.setSeriesShapesVisible(3, false)
    renderer.setSeriesPaint(3, Color.RED)

    val chart = ChartFactory.createScatterPlot(
        "Трансферы и голы нападающих",
        "Трансферная стоимость",
        "Голы",
        dataset
    )

    chart.xyPlot.renderer = renderer

    val frame = JFrame("Визуализация")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.add(ChartPanel(chart))
    frame.pack()
    frame.setSize(1200, 800)
    frame.isVisible = true
}