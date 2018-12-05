# !/usr/bin/env python
# -*- coding: utf-8 -*-

"""
..
	/////////////////////////////////////////////////////////////////////////
	//
	// (c) Copyright University of Southampton IT Innovation, 2015
	//
	// Copyright in this software belongs to IT Innovation Centre of
	// Gamma House, Enterprise Road, Southampton SO16 7NS, UK.
	//
	// This software may not be used, sold, licensed, transferred, copied
	// or reproduced in whole or in part in any manner or form or in or
	// on any media by any person other than in accordance with the terms
	// of the Licence Agreement supplied with the software, or otherwise
	// without the prior written consent of the copyright owners.
	//
	// This software is distributed WITHOUT ANY WARRANTY, without even the
	// implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
	// PURPOSE, except where stated in the Licence Agreement supplied with
	// the software.
	//
	// Created By : jessica Rosati
	// Created Date : 2017/06/27
	// Created for Project : GRAVITATE
	//
	/////////////////////////////////////////////////////////////////////////
	//
	// Dependencies : NONE
	//
	/////////////////////////////////////////////////////////////////////////
	'''
This module preprocesses literal values.  
For each predicate to process, it considers the .txt file with corresponding values and calls K-means algorithm (n_clusters=10). 
"""

import os, logging
from sklearn.cluster import KMeans
import re
import numpy as np

# date example values in BM RDF
# note: escaping of double quotes
# \"-900-01-01T00:00:00\"^^xsd:dateTime
# \"1475-01-01T00:00:00\"^^xsd:dateTime
# -2566-01-01
# -0501-01-01T00:00:00.000Z
# -450

# regex_date = re.compile( ur'"-?[0-9]*\D*?"' )
regex_date = re.compile( ur'\A(\\\"){0,1}(?P<YEAR>[\-\+]?[0-9]+)(?P<MONTH>\-[0-9]+){0,1}(?P<DAY>\-[0-9]+){0,1}(T[0-9/:/.Z]+){0,1}(\\\"){0,1}(\^\^xsd\:dateTime){0,1}\Z', re.UNICODE )

# unit examples
regex_unit = re.compile( ur'([0-9]*?.?[0-9]*)(\D*#)(\D*)' )


#def read_dimension_values_to_cluster(fname, values):
#	with open(fname) as f:
#		content = f.readlines()	
#		for line in content:
#			line = line.rstrip('\n')
#			m = re.match("\D*?\s*::\s*([0-9]*[,.]?[0-9]*?[\s*]?)([a-zA-Z]*)\s*[%]?\s*::\s*\D*?", line)
#			if(m.group(2) == 'cm'):
#				values[line] = float(m.group(1))
#			elif(m.group(2) == 'mm'):
#				values[line] = float(m.group(1))/10
#			elif(m.group(2) == 'm'):
#				values[line] = float(m.group(1))*10

#def read_date_values_to_cluster(fname, values):
#	with open(fname) as f:
#		content = f.readlines()
#		for line in content:
#			line = line.rstrip('\n')
#			if ('-' in line):
#				m = re.match("\D*?[\s*]?::[\s*]?([0-9]*[\s*]?)([a-zA-Z]*)[\s*]?[-][\s*]?([0-9]*[\s*]?)([a-zA-Z]*)[\s*]?::[\s*]?\D*?", line)
#				if m:
#					if(m.group(2).lower() == m.group(4).lower() == 'ad'):
#						values[line] = (float(m.group(1))+float(m.group(3)))/2.0
#					elif(m.group(2).lower() == m.group(4).lower() == 'bc'):
#						values[line] = - (float(m.group(1))+float(m.group(3)))/2.0
#					elif((m.group(2).lower() == 'bc') & (m.group(4).lower() == 'ad')):
#						if(float(m.group(1))>float(m.group(3))):
#							values[line] = - (float(m.group(1))+float(m.group(3)))/2.0
#						else:
#							values[line] = (float(m.group(1))+float(m.group(3)))/2.0 
#			else:
#				m = re.match("\D*?[\s*]?::[\s*]?([0-9]*[\s*]?)([a-zA-Z]*)[\s*]?::[\s*]?\D*?", line)
#				if m:
#					if(m.group(2).lower() == 'ad'):
#						values[line] = float(m.group(1))
#					elif(m.group(2).lower() == 'bc'):
#						values[line] = - (float(m.group(1)))

def read_date_values_to_cluster(fname, values, logger):
	"""Uses a regular expression to extract the date value.
	This method is suitable for predicates <http://www.cidoc-crm.org/cidoc-crm/P82a_begin_of_the_begin>	and <http://www.cidoc-crm.org/cidoc-crm/P82b_end_of_the_end>.

	:param fname: the name of the file with date values
	:type fname: string
	:param values
	:type: dict
	"""

	with open(fname) as f:
		content = f.readlines()
		for line in content:
			line = line.rstrip('\n')
			m = regex_date.match( line )
			if (m !=  None) and ('YEAR' in m.groupdict()) and ('MONTH' in m.groupdict()) and ('DAY' in m.groupdict()) :

				# compute date from regex extraction (year can be negative for BC)
				nYear = m.groupdict()['YEAR']
				if nYear == None :
					raise Exception( 'no year : ' + repr(line) )
				nYear = int( nYear )

				nMonth = m.groupdict()['MONTH']
				if nMonth == None :
					nMonth = 0
				else :
					nMonth = int(nMonth)

				nDay = m.groupdict()['DAY']
				if nDay == None :
					nDay = 0
				else :
					nDay = int(nDay)

				# compute a number for this date. Python datetime does not support dates before christ, so we just make a number up
				# based on the naieve  assumption of 365 days per year, 30 days per month
				# its only for clustering anyway!
				nDateValue = float( nYear * 365.0 + nMonth * 30.0 + nDay )

				# use this value (so we can cluster later on values)
				values[line] = ( nDateValue )
			else :
				logger.warn( 'bad date (ignored) : ' + repr(line) )

def read_unit_values_to_cluster(fname, values, logger):
	"""Uses a regular expression to extract the date value.
	This method is suitable for predicates <http://www.cidoc-crm.org/cidoc-crm/P82a_begin_of_the_begin>	and <http://www.cidoc-crm.org/cidoc-crm/P82b_end_of_the_end>.

	:param fname: the name of the file with date values
	:type fname: string
	:param values
	:type: dict
	"""
	with open(fname) as f:
		content = f.readlines()
		for line in content:
			line = line.rstrip('\n')

			# TODO UNIT GROUPS NEEDS WORK (revise regex and review possible units)

			m = regex_unit.match( line )
			if m :
				if(m.group(3)=='Centimeter'):
					values[line] = (float(m.group(1)))
				elif(m.group(3)=='Millimeter'):
					values[line] = (float(m.group(1))/10) 

if __name__ == "__main__":

	# make logger (global to STDOUT)
	LOG_FORMAT = ('%(levelname) -s %(asctime)s %(message)s')
	logger = logging.getLogger( __name__ )
	logging.basicConfig( level=logging.INFO, format=LOG_FORMAT )
	logger.info('logging started')

	clusters_file = open('preprocess_literal\\file_with_literal_cluster_entries.txt','a')
	for filename in os.listdir('preprocess_literal'):
		logger.info(filename)
		values = {}
		if(filename.startswith('date')):
			read_date_values_to_cluster('preprocess_literal\\'+filename, values, logger)

			# ignore literals where there are no examples
			if len(values) > 0 :

				# TODO hierarchical clustering would be better than k-means I think

				# decide on the k value (based on number of unique values for now)
				# values = { 'value_text_label' : numeric_value }
				nValuesTotal = len( values )
				nK = nValuesTotal / 20
				if nK < 1 :
					nK = 1
				elif nK > 20 :
					nK = 20

				# cluster values
				values_to_cluster = np.array(values.values()).reshape(-1, 1)	# reshape needed for passing 1d array to KMeans (the array is converted into a one-column matrix)
				kmeans = KMeans(n_clusters=nK, random_state=0).fit(values_to_cluster)
				i = 0
				for key in values:
					clusters_file.write(key+'\t')
					clusters_file.write(filename+"_cluster"+str(kmeans.labels_[i])+'\n')
					i = i+1
		elif(filename!='file_with_literal_cluster_entries.txt'):
			read_unit_values_to_cluster('preprocess_literal\\'+filename, values, logger)
			# ignore literals where there are no examples
			if len(values) > 0 :
				# decide on the k value (based on number of unique values for now)
				# values = { 'value_text_label' : numeric_value }
				nValuesTotal = len( values )
				nK = nValuesTotal / 100
				if nK < 1 :
					nK = 1
				elif nK > 100 :
					nK = 100

				values_to_cluster = np.array(values.values()).reshape(-1, 1)	# reshape needed for passing 1d array to KMeans (the array is converted into a one-column matrix)
				kmeans = KMeans(n_clusters=nK, random_state=0).fit(values_to_cluster)
				i = 0
				for key in values:
					clusters_file.write(key+'\t')
					clusters_file.write(filename+"_cluster"+str(kmeans.labels_[i])+'\n')
					i = i+1
	clusters_file.close()
