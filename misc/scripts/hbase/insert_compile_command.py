import os

directory = '/Users/McfateAlan/OathKeeper/conf/samples/hb-collections-large-gen'

for filename in os.listdir(directory):
    f_full = os.path.join(directory, filename)
    # checking if it is a file
    if os.path.isfile(f_full):
        print(f_full)
	filename = filename.replace('.properties','')
	f2 = open(f_full, 'a')  # open file in append mode
	f2.write('\n'+'''
compile_test_cmd="(echo 'Apply patch.' && git apply ${ok_dir}/conf/samples/hb-patches/FILENAME.patch) && \
mvn clean package -DskipTests"
'''.replace('FILENAME',filename))
	f2.close()
'''
for filename in os.listdir(directory):
    if filename.endswith(".properties"):
	f = open(file_name, 'a')  # open file in append mode
	f.write('\n'+)
	f.close()


with open('hbase-batch3.csv') as csv_file:
    csv_reader = csv.reader(csv_file, delimiter=',')
    for row in csv_reader:
'''
