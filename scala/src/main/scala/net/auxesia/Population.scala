/*
 * The MIT License
 * 
 * Copyright (c) 2011 John Svazic
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.auxesia

import akka.dispatch.Future

import scala.collection.Iterator
import scala.collection.immutable.Vector
import scala.math.round
import scala.util.Random

/**
 * Class defining a genetic algorithm population for the "Hello, world!" 
 * simulation.
 * 
 * @author John Svazic
 * @constructor Create a new population with an initial population defined, 
 * along with specific crossover, elitism and mutation rates.
 * @param _population The vector of [[net.auxesia.Chromosome]] objects 
 * representing the population.
 * @param crossover The crossover ratio.
 * @param elitism The elitism ratio.
 * @param mutation The mutation ratio.
 */
class Population private (private val popSize: Int, val crossover: Float, val elitism: Float, val mutation: Float) {
  private var _population = Population.generateInitialPopulation(popSize)

  /**
   * A public accessor for the underlying vector of [[net.auxesia.Chromosome]]
   * objects.
   */
  def population = _population
	
  /**
   * Method used to evolve a new generation for the population.  This method
   * modifies the internal population represented by this class.
   */
  def evolve = {
    // Create a buffer for the new generation
	def randomMutate(gene: String): String = {
	  if (Random.nextFloat() <= mutation) Chromosome.mutate(gene) else gene
	}
		
	def selectParents: Vector[Chromosome] = {
	  val tournamentSize = 3
	  var parents = Vector[Chromosome]()
	
	  // Randomly select two parents via tournament selection.
	  for (i <- 0 to 1) {
		var candidate = _population(Random.nextInt(popSize))
		for (j <- 1 to tournamentSize) {
		  val idx = Random.nextInt(popSize)
		  if (_population(idx).fitness < candidate.fitness) {
			candidate = _population(idx)
		  }
		}
		parents = parents :+ candidate
	  }
	  parents
	}
		
	val elitismCount = round(_population.size * elitism)
	var buffer = _population.take(elitismCount)
	var futures: Vector[Future[Any]] = Vector()
	for (idx <- elitismCount to _population.size - 1) {
	  if (Random.nextFloat() <= crossover) {
		// Select the parents and mate to get their children
		val parents  = selectParents
		futures = futures :+ Future { 
		  Chromosome.mate(parents(0).gene, parents(1).gene).map(randomMutate)
		}
	  } else {
	    futures = futures :+ Future { randomMutate(_population(idx).gene) }
	  }
	}
	
	def mkChromosome(f: Future[Any]): Vector[Chromosome]	= {
	  val fVal: Any = f.get
	  fVal match {
	    case v: Vector[String] => v.map(x => Chromosome(x))
		case gene: String => Vector(Chromosome(gene))
		case _ => Vector[Chromosome]()
	  }
	}
	
	futures.map(x => buffer = buffer ++ mkChromosome(x))

	// Grab the top population from the buffer.
	_population = buffer.sortWith((s, t) => s.fitness < t.fitness).take(popSize)
  }	
}

/**
 * Factory for [[net.auxesia.Population]] instances.
 */
object Population {
  /**
   * Create a [[net.auxesia.Population]] with a given size, crossover ratio, elitism ratio
   * and mutation ratio.
   * 
   * @param size The size of the population.
   * @param crossover The crossover ratio for the population.
   * @param elitism The elitism ratio for the population.
   * @param mutation The mutation ratio for the population.
   * 
   * @return A new [[net.auxesia.Population]] instance with the defined
   * parameters and an initialized set of [[net.auxesia.Chromosome]] objects
   * representing the population.
   */
  def apply(size: Int, crossover: Float, elitism: Float, mutation: Float) = {
	new Population(size, crossover, elitism, mutation)
  }
	
  /**
   * Helper method used to generate an initial population of random
   * [[net.auxesia.Chromosome]] objects for a given population size.
   * 
   * @param size The size of the population.
   * 
   * @return A [[scala.collection.immutable.List]] of the defined size
   * populated with random [[net.auxesia.Chromosome]] objects.
   */
  private def generateInitialPopulation(size: Int): Vector[Chromosome] = {
	Vector.fill(size)(Chromosome(Chromosome.generateRandomGene)).sortWith(
	    (s, t) => s.fitness < t.fitness
	)
  }
}