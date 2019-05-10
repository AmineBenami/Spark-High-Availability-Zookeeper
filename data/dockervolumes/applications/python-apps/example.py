import pyspark

print(pyspark.SparkContext().parallelize(range(0, 1000)).count())
