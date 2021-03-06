package module.taiex
/**
TAIEX  發行量加權股價指數歷史資料
*/
class Taiex{
	def sqlDirName = 'taiex'
	def dbName = 'findb'
	def tableName = 'taiex'
	def doSync(){
///////////////
		module.processor.ProcessorRunner.runMonthByMonth{
			startYear Calendar.getInstance().get(Calendar.YEAR)//Integer.valueOf(listing_day.toString()[0..3])
			startMonth Calendar.getInstance().get(Calendar.MONTH)+1//Integer.valueOf(listing_day.toString()[4..5])
			startday 1//Integer.valueOf(listing_day.toString()[6..7])
			endYear Calendar.getInstance().get(Calendar.YEAR)
			endMonth Calendar.getInstance().get(Calendar.MONTH)+1
			endDay 1
			process{yyyyMmDd->
						 def z = [2330,2340,2350]
						Random rnd = new Random()
						def w = z[rnd.nextInt(z.size())]
						println 'wait'+ w
						sleep(w)
				def resultSql =''
				if(!new File("./${sqlDirName}/${yyyyMmDd}.sql").exists()){
				    def returnJson = module.web.Webget.download{
				         url "https://www.twse.com.tw/indicesReport/MI_5MINS_HIST?response=json&lang=en&date=${yyyyMmDd}"
				         decode 'utf-8'
				         validateTaiex true
				    }

					module.parser.JsonConvert.convert{
			        	input returnJson
			        	parseRule {json->
				            if(json.stat != 'OK'){
				               return null
				            }
				            def fields = json.fields.collect(fieldNormalize)
				            if(resultSql==''){
				            	resultSql = "REPLACE INTO `${dbName}`.`${tableName}` (`${fields.join('`,`')}`,`traded_day`) VALUES "
				            }
				            for(int i=0;i<json.data.size();i++){
				                def _data = json.data[i].collect(valueNormalise).join("','");
				                if(resultSql.endsWith('VALUES ')){
				                  resultSql+="\r\n('${_data}','${yyyyMmDd}')"
				               }else{
				                  resultSql+="\r\n,('${_data}','${yyyyMmDd}')"
				               }
				            }
			        	}
			    	}//convert
			    	if(resultSql && !resultSql.endsWith('VALUES ')){
						module.io.Batch.exec{
                				mkdirs "./${sqlDirName}/"
                				write "./${sqlDirName}/${yyyyMmDd}.sql",'UTF-8',"${resultSql};"
           				}
           				print '*'
					}
				}else{
					print '>'
				}
			}//process
		}//run
		module.db.SqlExecuter.exec{
		    dir "./${sqlDirName}/"
		}
		module.io.Batch.exec{
			clean "./${sqlDirName}/"
			delete "./${sqlDirName}/"
			info 'import taiex done'
		}
///////////////
	}
	def static sync(@DelegatesTo(Taiex) Closure block){
	        Taiex m = new Taiex()
	        block.delegate = m
	        block()
	        m.doSync()
    }
}