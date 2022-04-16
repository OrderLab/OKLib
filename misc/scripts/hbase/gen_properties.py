import csv

with open('hbase-batch3.csv') as csv_file:
    csv_reader = csv.reader(csv_file, delimiter=',')
    for row in csv_reader:
	file_name = 'recipes/'+row[0].replace(' ','')+'.properties'
	f = open(file_name, 'w')  # open file in append mode
	f.write('commit_id='+row[2].replace('https://github.com/apache/hbase/commit/',''))
	f.close()
