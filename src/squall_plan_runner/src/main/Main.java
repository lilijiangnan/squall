package main;

import backtype.storm.Config;
import backtype.storm.topology.TopologyBuilder;
import org.apache.log4j.Logger;

import components.Component;
import java.util.List;
import java.util.Map;
import queryPlans.debug.HyracksL1Plan;
import queryPlans.debug.HyracksL3BatchPlan;
import queryPlans.debug.HyracksL3Plan;
import queryPlans.HyracksPlan;
import queryPlans.HyracksPreAggPlan;
import queryPlans.QueryPlan;
import queryPlans.RSTPlan;
import queryPlans.TPCH10Plan;
import queryPlans.debug.TPCH3L1Plan;
import queryPlans.debug.TPCH3L23Plan;
import queryPlans.debug.TPCH3L2Plan;
import queryPlans.TPCH3Plan;
import queryPlans.TPCH4Plan;
import queryPlans.TPCH5Plan;
import queryPlans.TPCH7Plan;
import queryPlans.TPCH8Plan;
import queryPlans.TPCH9Plan;
import queryPlans.ThetaHyracksPlan;
import queryPlans.ThetaInputDominatedPlan;
import queryPlans.ThetaMultipleJoinPlan;
import queryPlans.ThetaOutputDominatedPlan;
import queryPlans.ThetaTPCH7Plan;
import stormComponents.StormJoin;
import stormComponents.StormComponent;
import stormComponents.synchronization.TopologyKiller;
import utilities.StormWrapper;
import utilities.SystemParameters;


public class Main {
	private static Logger LOG = Logger.getLogger(Main.class);

        public static void main(String[] args) {
           new Main(args);
        }

        public Main(String[] args){
            String propertiesPath = args[0];
            Config conf = SystemParameters.fileToStormConfig(propertiesPath);

            QueryPlan queryPlan = chooseQueryPlan(conf);
            TopologyBuilder builder = createTopology(queryPlan, conf);
            StormWrapper.submitTopology(conf, builder);
        }

        public Main(QueryPlan queryPlan, Map map){
            Config conf = SystemParameters.mapToStormConfig(map);
            TopologyBuilder builder = createTopology(queryPlan, conf);
            StormWrapper.submitTopology(conf, builder);
        }

        private static TopologyBuilder createTopology(QueryPlan qp, Config conf) {
            TopologyBuilder builder = new TopologyBuilder();
            TopologyKiller killer = new TopologyKiller(builder);

            //DST_ORDERING is the optimized version, so it's used by default
            int partitioningType = StormJoin.DST_ORDERING;

            List<Component> queryPlan = qp.getPlan();
            List<String> allCompNames = qp.getComponentNames();
            int planSize = queryPlan.size();
            for(int i=0;i<planSize;i++){
                Component component = queryPlan.get(i);
                if(component.getChild() == null){
                    //a last component (it might be multiple of them)
                    component.makeBolts(builder, killer, allCompNames, conf, partitioningType, StormComponent.FINAL_COMPONENT);
                }else{
                    component.makeBolts(builder, killer, allCompNames, conf, partitioningType, StormComponent.INTERMEDIATE);
                }  
            }

            // printing infoID information and returning the result
            //printInfoID(killer, queryPlan); commented out because IDs are now desriptive names
            return builder;
        }

        private static void printInfoID(TopologyKiller killer,
                List<Component> queryPlan) {

            StringBuilder infoID = new StringBuilder("\n");
            if(killer!=null){
                infoID.append(killer.getInfoID());
                infoID.append("\n");
            }
            infoID.append("\n");

            // after creating bolt, ID of a component is known
            int planSize = queryPlan.size();
            for(int i=0;i<planSize;i++){
                Component component = queryPlan.get(i);
                infoID.append(component.getInfoID());
                infoID.append("\n\n");
            }

            LOG.info(infoID.toString());
        }


        public static QueryPlan chooseQueryPlan(Map conf){
            String queryName = SystemParameters.getString(conf, "DIP_QUERY_NAME");
            //if "/" is the last character, adding one more is not a problem
            String dataPath = SystemParameters.getString(conf, "DIP_DATA_PATH") + "/";
            String extension = SystemParameters.getString(conf, "DIP_EXTENSION");

            QueryPlan queryPlan = null;

            // change between this and ...
            if(queryName.equalsIgnoreCase("rst")){
                queryPlan = new RSTPlan(dataPath, extension, conf).getQueryPlan();
            }else if (queryName.equalsIgnoreCase("hyracks")){
                queryPlan = new HyracksPlan(dataPath, extension, conf).getQueryPlan();
            }else if (queryName.equalsIgnoreCase("hyracks_pre_agg")){
                queryPlan = new HyracksPreAggPlan(dataPath, extension, conf).getQueryPlan();
            }else if (queryName.equalsIgnoreCase("hyracks_l1")){
                queryPlan = new HyracksL1Plan(dataPath, extension, conf).getQueryPlan();
            }else if (queryName.equalsIgnoreCase("hyracks_l3")){
                queryPlan = new HyracksL3Plan(dataPath, extension, conf).getQueryPlan();
            }else if (queryName.equalsIgnoreCase("hyracks_l3_batch")){
                queryPlan = new HyracksL3BatchPlan(dataPath, extension, conf).getQueryPlan();
            }else if(queryName.equalsIgnoreCase("tpch3")){
                queryPlan = new TPCH3Plan(dataPath, extension, conf).getQueryPlan();
            }else if(queryName.equalsIgnoreCase("tcph3_l1")){
                queryPlan = new TPCH3L1Plan(dataPath, extension, conf).getQueryPlan();
            }else if(queryName.equalsIgnoreCase("tpch3_l2")){
                queryPlan = new TPCH3L2Plan(dataPath, extension, conf).getQueryPlan();
            }else if(queryName.equalsIgnoreCase("tpch3_l23")){
                queryPlan = new TPCH3L23Plan(dataPath, extension, conf).getQueryPlan();
            }else if(queryName.equalsIgnoreCase("tpch4")){
                queryPlan = new TPCH4Plan(dataPath, extension, conf).getQueryPlan();
            }else if(queryName.equalsIgnoreCase("tpch5")){
                queryPlan = new TPCH5Plan(dataPath, extension, conf).getQueryPlan();
            }else if(queryName.equalsIgnoreCase("tpch7")){
                queryPlan = new TPCH7Plan(dataPath, extension, conf).getQueryPlan();
            }else if(queryName.equalsIgnoreCase("tpch8")){
                queryPlan = new TPCH8Plan(dataPath, extension, conf).getQueryPlan();
            }else if(queryName.equalsIgnoreCase("tpch9")){
                queryPlan = new TPCH9Plan(dataPath, extension, conf).getQueryPlan();
            }else if(queryName.equalsIgnoreCase("tpch10")){
                queryPlan = new TPCH10Plan(dataPath, extension, conf).getQueryPlan();
            }else if (queryName.equalsIgnoreCase("theta_tpch7")){
            	queryPlan = new ThetaTPCH7Plan(dataPath, extension, conf).getQueryPlan();
            }else if (queryName.equalsIgnoreCase("theta_input_dominated")){
            	queryPlan = new ThetaInputDominatedPlan(dataPath, extension, conf).getQueryPlan();
            }else if (queryName.equalsIgnoreCase("theta_output_dominated")){
            	queryPlan = new ThetaOutputDominatedPlan(dataPath, extension, conf).getQueryPlan();
            }else if (queryName.equalsIgnoreCase("theta_multiple_join")){
            	queryPlan = new ThetaMultipleJoinPlan(dataPath, extension, conf).getQueryPlan();
            }else if (queryName.equalsIgnoreCase("theta_hyracks")){
            	queryPlan = new ThetaHyracksPlan(dataPath, extension, conf).getQueryPlan();
            }
            // ... this line

            if (queryPlan == null){
                throw new RuntimeException("QueryPlan " + queryName + " doesn't exist in Main.java");
            }
            return queryPlan;
        }
}