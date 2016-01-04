#!/usr/bin/env python
# -*- coding: utf-8 -*-
# @Author: Zeyuan Shang
# @Date:   2016-01-04 13:49:11
# @Last Modified by:   Zeyuan Shang
# @Last Modified time: 2016-01-04 14:51:52
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
        avg.append((total / cnt, -int(movieId)))
    avg = sorted(avg, reverse = True)

    for i in xrange(10):
        movieId = str(-avg[i][1])
        mean = avg[i][0]
        title = movies_stats[movieId]
        print mean, movieId, movies_stats[movieId]

def top_by_damped_mean(ratings, movies):
    print 'TOP MOVIES BY DAMPED MEAN'
    
    ratings_stats = {}
    for userId, movieId, rating, timestamp in ratings:
        if float(rating) == 5.0:
            continue
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
        avg.append((total / cnt, -int(movieId)))
    avg = sorted(avg, reverse = True)

    for i in xrange(10):
        movieId = str(-avg[i][1])
        mean = avg[i][0]
        title = movies_stats[movieId]
        print mean, movieId, movies_stats[movieId]

def top_by_number_of_ratings(ratings, movies):
    print 'TOP MOVIES BY NUMBER OF RATINGS'
    
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
        avg.append((cnt, -int(movieId)))
    avg = sorted(avg, reverse = True)

    for i in xrange(10):
        movieId = str(-avg[i][1])
        cnt, total = ratings_stats[movieId]
        mean = total / cnt
        title = movies_stats[movieId]
        print mean, movieId, movies_stats[movieId]

def top_by_simple_similarity(ratings, movies, special_movieId):
    print 'TOP MOVIES FOR Jaws 3-D (SIMPLE)'

    users_stats = {}
    for userId, movieId, rating, timestamp in ratings:
        if not userId in users_stats:
            users_stats[userId] = []
        users_stats[userId].append(movieId)
    movies_stats = {}
    for movieId, title, genres in movies:
        movies_stats[movieId] = title

    movies_cnt = {}
    for userId in users_stats:
        rated_movies = users_stats[userId]
        if special_movieId in rated_movies:
            for movie in rated_movies:
                movies_cnt[movie] = movies_cnt.get(movie, 0) + 1
    scores = []
    for movie in movies_cnt:
        if movie == special_movieId:
            continue
        scores.append((float(movies_cnt[movie]) / movies_cnt[special_movieId], -int(movie)))
    scores = sorted(scores, reverse = True)

    for i in xrange(10):
        movieId = str(-scores[i][1])
        score = scores[i][0]
        title = movies_stats[movieId]
        print score, movieId, movies_stats[movieId]

if __name__ == "__main__":
    # ratings: userId, movieId, rating, timestamp
    ratings = read_csv_file(RATINGS_CSV_FILE)
    # movies: movieId, title, genres
    movies = read_csv_file(MOVIES_CSV_FILE)

    # work
    top_by_mean(ratings, movies)
    print 
    top_by_damped_mean(ratings, movies)
    print
    top_by_number_of_ratings(ratings, movies)
    print
    top_by_simple_similarity(ratings, movies, '1389')




