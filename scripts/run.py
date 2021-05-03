import os
import subprocess
import threading
from datetime import datetime
from pathlib import Path
from random import randrange

from elasticsearch import Elasticsearch
from elasticsearch_dsl import Search


class runner (threading.Thread):
    def __init__(self, name):
        threading.Thread.__init__(self)
        self.name = name
        
    def run(self):
        print("Starting:" + self.name)
        executeJarRunner()
        print("Exiting:"+ self.name)


def getJarFile():
    base = Path(__file__).parent.parent / "target"
    for file in os.listdir(base):
        if "shaded" in file:
            return base / file

def executeJarRunner():
    ret = executeJar()
    if ret != 0:
        print("failed")
    else:
        print("success")


def randomiseVariables():
    # argument_list = []
    argument_dict = {
        "pg" : randrange(1,10),
        "pd" : randrange(1,10),
        "sh" : randrange(1,50),
        "ev" : randrange(1,10)
    }
    
    # pg = randrange(1,20)
    # pd = randrange(1,10)
    # sh = randrange(1,100)
    # ev = randrange(1,20)
    # argument_list.append(pg,pd,sh,ev)
    print(argument_dict)
    return argument_dict

def executeJar():
    var_dict = randomiseVariables()
    #flags
    # powerGenAgent = {'-p' : 1}
    # powerDisAgent = {'-pd' : 1}
    # smartHomeAgent = {'-sh' : 5}
    # evAgent = {'-ev' : 2}
    # runTime = {'-rt' : 60}
    # waitTime = {'-wt' : 2000}
    
    # powergen = '-pd'
    # pg = '1'
    # powerdi = '-pd'
    # pd = '1'
    # smarthome = '-sh'
    # sh = '6'
    # electronicveh = '-ev'
    # ev = '2'
    
    powergen = '-pg'
    pg = str(var_dict["pg"])
    powerdi = '-pd'
    pd = str(var_dict["pd"])
    smarthome = '-sh'
    sh = str(var_dict["sh"])
    electronicveh = '-ev'
    ev = str(var_dict["ev"])
    runtime = '-rt'
    rt = '120'
    wait = '-wt'
    wt = '2000'
    
    indexName = '-indexname'
    now = datetime.now()
    dt_string = now.strftime("smartgrid-%Y-%m-%dt%H.%M.%S.%f")
    # indexName = {'-indexname' : dt_string}
    
    testrun = '-testrun'
    testname = 'consistentreliabilitythreshold'
    
    elastichost = '-elastichost'
    ipaddr = '192.168.25.3'
    
    jarfile = getJarFile()
    ret = subprocess.call(['java', '-jar', jarfile, powergen, pg, powerdi, pd, smarthome, sh, electronicveh, ev, runtime, rt, wait, wt, indexName, dt_string, testrun, testname])
    return ret


def queryES(index):
    client = Elasticsearch(['http://localhost:9200/'])
    s = Search(using=client, index=index)
    request = s.source(['hash', 'author_date', 'author'])
        
    response = s.scan()

    # print(response.to_dict())

    # for hit in response:
    #     print(hit.to_dict())
    
    return response
        

def queryESALL(index):
    # ElasticSearch instance (url)
    es = Elasticsearch(['http://localhost:9200/'])

    # Build a DSL Search object on the 'commits' index, 'summary' document type
    request = Search(using=es, index=index, doc_type='summary')

    # Run the Search, using the scan interface to get all resuls
    response = request.scan()
    for commit in response:
        print(commit)
        
def checkMRConsistentPowerRegulation(index):
    response = queryES(index)
    returnValue = False;
    for hit in response:
        values = hit.to_dict()
        if 'SoSAgent' in values.values():
            if 'action' in values:
                action = values['action']
                if action == 'sosagent.regulate_power':
                    if values['regulate_power'] == 'true':
                        returnValue = True
                        break
    
    print("MRConsistentPowerRegulation --", index, "-- result:", returnValue)
    return returnValue


def main():
    
    # thread_list = []
    
    # for x in range(4):
    #     thread = runner(x)
    #     thread_list.append(thread)
    # for thread in thread_list:
    #     thread.start()
    #     print("Starting Threads")
    # for thread in thread_list:
    #     thread.join()
    print("hello")

    checkMRConsistentPowerRegulation('smartgridsos')
    # queryESALL('smartgridsos')
    

if __name__ == '__main__':
    main()
