#!/usr/bin/env python
# -*- coding: utf-8 -*-
# @Author: Zeyuan Shang
# @Date:   2016-01-04 13:49:11
# @Last Modified by:   Zeyuan Shang
# @Last Modified time: 2016-01-04 14:11:09
import csv

RATINGS_CSV_FILE = 'ml-latest-small/ratings.csv'
MOVIES_CSV_FILE = 'ml-latest-small/movies.csv'

def read_csv_file(filename):
    csv_file = open(filename, 'rb')
    csv_reader = csv.reader(csv_file, delimiter=',', quotechar='"')
    return list(csv_reader)[1:]

def top_by_mean(ratings, movies):
    print 'TOP MOVIES BY MEAN'
    
    ratings_stats = {}
    for userId, movieId, rating, timestamp in ratings:
        if movieId in ratings_stats:
            cnt, total = ratings_stats[movieId]
            cnt += 1
            total += float(rating)
            ratings_stats[movieId] = (cnt, total)
        else:
            ratings_stats[movieId] = (1, float(rating))
    movies_stats = {}
    for movieId, title, genres in movies:
        movies_stats[movieId] = title

    avg = []
    for movieId in ratings_stats:
        cnt, total = ratings_stats[movieId]
        avg.append((total / cnt, int(movieId)))
    avg = sorted(avg, reverse = True)

    for i in xrange(10):
        print avg[i][0], avg[i][1], movies_stats[str(avg[i][1])]

if __name__ == "__main__":
    # ratings: userId, movieId, rating, timestamp
    ratings = read_csv_file(RATINGS_CSV_FILE)
    # movies: movieId, title, genres
    movies = read_csv_file(MOVIES_CSV_FILE)

    # work
    top_by_mean(ratings, movies)



