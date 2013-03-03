package dk.bayes.gaussian

import scala.Math.Pi
import scala.Math.exp
import scala.Math.pow
import scala.Math.sqrt
import dk.bayes.gaussian.Linear.Matrix
import org.apache.commons.math3.distribution.NormalDistribution
import Gaussian._

/**
 * Univariate Gaussian Distribution.
 *
 * @author Daniel Korzekwa
 *
 * @param m Mean
 * @param v Variance
 */
case class Gaussian(m: Double, v: Double) {

  def pdf(x: Double): Double = Gaussian.pdf(x, m, v)

  def cdf(x: Double) = Gaussian.cdf(x, m, v)

  /**
   * Returns upper tail gaussian truncation.
   * http://en.wikipedia.org/wiki/Truncated_normal_distribution
   */
  def truncateUpperTail(x: Double): Gaussian = {

    def lambda(alpha: Double): Double = stdPdf(alpha) / (1 - stdCdf(alpha))
    def delta(alpha: Double): Double = lambda(alpha) * (lambda(alpha) - alpha)

    val sd = sqrt(v)
    val alpha = (x - m) / sd

    val truncatedMean = m + sd * lambda(alpha)
    val truncatedVariance = v * (1 - delta(alpha))

    Gaussian(truncatedMean, truncatedVariance)
  }

  def *(linearGaussian: LinearGaussian): MultivariateGaussian = {
    val m = Matrix(this.m, linearGaussian.a * this.m + linearGaussian.b)

    val R = Matrix(2, 2)
    R.set(0, 0, 1 / this.v + pow(linearGaussian.a, 2) * (1 / linearGaussian.v))
    R.set(0, 1, -linearGaussian.a * (1 / linearGaussian.v))
    R.set(1, 0, (-1 / linearGaussian.v) * linearGaussian.a)
    R.set(1, 1, 1 / linearGaussian.v)

    val v = R.inv

    MultivariateGaussian(m, v)
  }

  /**
   * P.A. Bromiley. Products and Convolutions of Gaussian Distributions, 2003
   */
  def *(gaussian: Gaussian): Gaussian = {

    val product = if (gaussian.v == Double.PositiveInfinity) this else {
      val newM = (m * gaussian.v + gaussian.m * v) / (v + gaussian.v)
      val newV = (v * gaussian.v) / (v + gaussian.v)
      Gaussian(newM, newV)
    }

    product
  }

  /**
   * Thomas Minka. EP: A quick reference, 2008
   */
  def /(gaussian: Gaussian): Gaussian = {
    val newV = 1 / (1 / v - 1 / gaussian.v)
    val newM = newV * (m / v - gaussian.m / gaussian.v)

    Gaussian(newM, newV)
  }

  /**
   * P.A. Bromiley. Products and Convolutions of Gaussian Distributions, 2003
   */
  def +(gaussian: Gaussian): Gaussian = {
    val newM = m + gaussian.m
    val newV = v + gaussian.v
    Gaussian(newM, newV)
  }

  /**
   * http://mathworld.wolfram.com/NormalDifferenceDistribution.html
   */
  def -(gaussian: Gaussian): Gaussian = this + gaussian.copy(m = -gaussian.m)

  /**
   * Returns the derivative value of Gaussian with respect to mean, evaluated at the value of x.
   */
  def derivativeM(x: Double): Double = pdf(x) * (x - m) / v

  /**
   * Returns the derivative value of Gaussian with respect to variance, evaluated at the value of x.
   */
  def derivativeV(x: Double): Double = pdf(x) * (1d / (2 * v * v) * (x - m) * (x - m) - 1d / (2 * v))
}

object Gaussian {

  /**
   * Returns the value of probability density function of the normal distribution.
   *
   * @param x The pdf function is evaluated at the value of x
   * @param m Mean
   * @param v Variance
   */
  def pdf(x: Double, m: Double, v: Double) = normConstant(v) * exp(-pow(x - m, 2) / (2 * v))

  /**
   * Returns the normalisation constant of the Gaussian distribution.
   *
   * @param v Variance
   */
  def normConstant(v: Double): Double = 1 / sqrt(2 * Pi * v)

  /**
   * Returns the value of probability density function of the standard normal distribution.
   */
  def stdPdf(x: Double): Double = pdf(x, 0, 1)

  /**
   * Returns the value of cumulative distribution function
   *
   * @param x The cdf function is evaluated at the value of x
   * @param m Mean
   * @param v Variance
   */
  def cdf(x: Double, m: Double, v: Double): Double = new NormalDistribution(m, sqrt(v)).cumulativeProbability(x)

  /**
   * Returns the value of cumulative distribution function of the standard normal distribution.
   */
  def stdCdf(x: Double): Double = cdf(x, 0, 1)

  /**
   * Projects histogram to Gaussian distribution by matching the mean and variance moments.
   */
  def projHistogram(values: Seq[Double], probs: Seq[Double]): Gaussian = {
    require(values.size == probs.size, "Number of values is not equal to number of probabilities")

    val Z = probs.sum
    val m = values.zip(probs).map { case (v, p) => v * p }.sum / Z
    val mm = values.zip(probs).map { case (v, p) => v * v * p }.sum / Z
    Gaussian(m, mm - m*m)
  }

}