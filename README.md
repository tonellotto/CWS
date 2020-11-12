# Consistent Weighted Sampling

This software implements *consistent weighted sampling (CWS)*, a similarity-preserving hashing technique for weighted Jaccard (or min-max) similarity, and approximate nearest neighbor (ANN) search via CWS.
The software applies a simplification of the original CWS method called that generates non-negative integer vectors of sample ids, i.e., the sampled weights are not stored.

## Build instruction

You can download and compile the software as follows.

```
$ git clone https://github.com/tonellotto/CWS
$ cd CWS
$ mvn clean package
```

## Input file format

The software supports the [LIBSVM format](https://www.csie.ntu.edu.tw/~r94100/libsvm-2.8/README) whose each feature vector is written in ASCII, as follows.

```
<label> <index1>:<value1> <index2>:<value2> <index3>:<value3> ...
<label> <index1>:<value1> <index2>:<value2> <index3>:<value3> ...
.
.
.
<label> <index1>:<value1> <index2>:<value2> <index3>:<value3> ...
```

## Running example

Move to the `target/bin` folder, containing the executable scripts generated during the build process.


### Download the dataset

* Create the `news20` folder to store the dataset files:

	```
	$ mkdir news20
	```

* Download the dataset `news20.scale.bz2` to be used as input:

	```
	$ wget https://www.csie.ntu.edu.tw/~cjlin/libsvmtools/datasets/multiclass/news20.scale.bz2
	$ bzip2 -d news20.scale.bz2
	$ mv news20.scale news20/news20.scale_base.txt
	```

* Download the dataset `news20.t.scale.bz2` and extract the first 100 feature vectors to be used as a query collection:

	```
	$ wget https://www.csie.ntu.edu.tw/~cjlin/libsvmtools/datasets/multiclass/news20.t.scale.bz2
	$ bzip2 -d news20.t.scale.bz2
	$ head -100 news20.t.scale > news20/news20.scale_query.txt
	$ rm -f news20.t.scale
	```

As a result, there should be the input file `news20.scale_base.txt` and query collection file `news20.scale_query.txt` in the folder `news20`.

### Generate the CWS vectors from the input

```
$ ./cws-main -i news20/news20.scale_base.txt -o news20/news20.scale_base.cws -d 62062 -D 64 -w -l
1) Generate random matrix data...
Elapsed time: 00:00.606
The random matrix data consumes 90,91 MiB
2) Do consistent weighted sampling...
15935 vectors processed in 00:17.480
Completed!! Processed 0,02 millions of elements in 00:17.482
```

As a result, there should be the CWS data file `news20/news20.scale_base.cws`.

### Generate the CWS vectors from the query collection

```
$ ./cws-main -i news20/news20.scale_query.txt -o news20/news20.scale_query.cws -d 62062 -D 64 -w -l
1) Generate random matrix data...
Elapsed time: 00:00.656
The random matrix data consumes 90,91 MiB
2) Do consistent weighted sampling...
100 vectors processed in 00:00.264
Completed!! Processed 0,00 millions of elements in 00:00.264
```

As a result, there should be the CWS data file `news20/news20.scale_query.cws.bvecs`.

### Make the ground truth data in (weighted) Jaccard similarity

To evaluate the ANN search, create the ground truth data in (weighted) Jaccard similarity from `news20.scale_base.txt` and `news20.scale_query.txt`.

```
./ground-truth -i news20/news20.scale_base.txt -q news20/news20.scale_query.txt -o news20/news20.scale_groundtruth -w -l
Completed!! Processed 100 queries in 00:02.156
Output in news20/news20.scale_query.txt
```

As a result, there should be the ground truth file `news20/news20.scale_groundtruth`.

### Perform ANN search

Search ANN vectors from the database `news20.scale_base.cws` for each query vector in `news20.scale_query.cws`.

```
./ann-search -i news20/news20.scale_base.cws -q news20/news20.scale_query.cws -o news20/news20.scale_score -k 100
Output in news20/news20.scale_score
```

As a result, there should be the result file `news20/news20.scale_score`.

### Evaluate the recall

Evaluate the recalls for the search results.

```
../../scripts/evaluate.py news20/news20.scale_score news20/news20.scale_groundtruth
Recall@1:	0.571
Recall@2:	0.704
Recall@5:	0.786
Recall@10:	0.847
Recall@20:	0.878
Recall@50:	0.918
Recall@100:	0.918
```