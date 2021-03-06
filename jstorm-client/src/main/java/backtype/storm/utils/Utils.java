package backtype.storm.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.io.input.ClassLoaderObjectInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.thrift7.TException;
import org.json.simple.JSONValue;
import org.yaml.snakeyaml.Yaml;

import backtype.storm.Config;
import backtype.storm.generated.ComponentCommon;
import backtype.storm.generated.ComponentObject;
import backtype.storm.generated.StormTopology;
import clojure.lang.IFn;
import clojure.lang.RT;

import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;
import com.netflix.curator.retry.ExponentialBackoffRetry;

public class Utils {
	public static final String DEFAULT_STREAM_ID = "default";

	public static Object newInstance(String klass) {
		try {
			Class c = Class.forName(klass);
			return c.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static byte[] serialize(Object obj) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(obj);
			oos.close();
			return bos.toByteArray();
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	public static Object deserialize(byte[] serialized, URLClassLoader loader) {
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
			Object ret = null;
			if (loader != null) {
				ClassLoaderObjectInputStream cis = new ClassLoaderObjectInputStream(
						loader, bis);
				ret = cis.readObject();
				cis.close();
			} else {
				ObjectInputStream ois = new ObjectInputStream(bis);
				ret = ois.readObject();
				ois.close();
			}
			return ret;
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static Object deserialize(byte[] serialized) {
		return deserialize(serialized, WorkerClassLoader.getInstance());
	}

	public static <T> String join(Iterable<T> coll, String sep) {
		Iterator<T> it = coll.iterator();
		String ret = "";
		while (it.hasNext()) {
			ret = ret + it.next();
			if (it.hasNext()) {
				ret = ret + sep;
			}
		}
		return ret;
	}

	public static void sleep(long millis) {
		try {
			Time.sleep(millis);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public static List<URL> findResources(String name) {
		try {
			Enumeration<URL> resources = Thread.currentThread()
					.getContextClassLoader().getResources(name);
			List<URL> ret = new ArrayList<URL>();
			while (resources.hasMoreElements()) {
				ret.add(resources.nextElement());
			}
			return ret;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Map findAndReadConfigFile(String name, boolean mustExist) {
		try {
			HashSet<URL> resources = new HashSet<URL>(findResources(name));
			if (resources.isEmpty()) {
				if (mustExist)
					throw new RuntimeException(
							"Could not find config file on classpath " + name);
				else
					return new HashMap();
			}
			if (resources.size() > 1) {
				throw new RuntimeException(
						"Found multiple "
								+ name
								+ " resources. You're probably bundling the Storm jars with your topology jar. "
								+ resources);
			}
			URL resource = resources.iterator().next();
			Yaml yaml = new Yaml();
			Map ret = (Map) yaml.load(new InputStreamReader(resource
					.openStream()));
			if (ret == null)
				ret = new HashMap();

			return new HashMap(ret);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Map findAndReadConfigFile(String name) {
		return findAndReadConfigFile(name, true);
	}

	public static Map readDefaultConfig() {
		return findAndReadConfigFile("defaults.yaml", true);
	}

	public static Map readCommandLineOpts() {
		Map ret = new HashMap();
		String commandOptions = System.getProperty("storm.options");
		if (commandOptions != null) {
			commandOptions = commandOptions.replaceAll("%%%%", " ");
			String[] configs = commandOptions.split(",");
			for (String config : configs) {
				String[] options = config.split("=");
				if (options.length == 2) {
					ret.put(options[0], options[1]);
				}
			}
		}
		return ret;
	}
	
	public static void replaceLocalDir(Map<Object, Object> conf) {
		String stormHome = System.getProperty("jstorm.home");
		boolean isEmpty = StringUtils.isBlank(stormHome);
		
		Map<Object, Object> replaceMap = new HashMap<Object, Object>();
		
		for (Entry entry : conf.entrySet()) {
			Object key = entry.getKey();
			Object value = entry.getValue();
			
			if (value instanceof String) {
				if (StringUtils.isBlank((String)value) == true ) {
					continue;
				}
				
				String str = (String)value;
				if (isEmpty == true) {
				    // replace %JSTORM_HOME% as current directory
				    str = str.replace("%JSTORM_HOME%", ".");
				}else {
				    str = str.replace("%JSTORM_HOME%", stormHome);
				}
				
				
				replaceMap.put(key, str);
			}
		}
		
		
		conf.putAll(replaceMap);
	}

	public static Map readStormConfig() {
		Map ret = readDefaultConfig();
		String confFile = System.getProperty("storm.conf.file");
		Map storm;
		if (confFile == null || confFile.equals("")) {
			storm = findAndReadConfigFile("storm.yaml", false);
		} else {
			storm = findAndReadConfigFile(confFile, true);
		}
		ret.putAll(storm);
		ret.putAll(readCommandLineOpts());
		
		replaceLocalDir(ret);
		return ret;
	}

	private static Object normalizeConf(Object conf) {
		if (conf == null)
			return new HashMap();
		if (conf instanceof Map) {
			Map confMap = new HashMap((Map) conf);
			for (Object key : confMap.keySet()) {
				Object val = confMap.get(key);
				confMap.put(key, normalizeConf(val));
			}
			return confMap;
		} else if (conf instanceof List) {
			List confList = new ArrayList((List) conf);
			for (int i = 0; i < confList.size(); i++) {
				Object val = confList.get(i);
				confList.set(i, normalizeConf(val));
			}
			return confList;
		} else if (conf instanceof Integer) {
			return ((Integer) conf).longValue();
		} else if (conf instanceof Float) {
			return ((Float) conf).doubleValue();
		} else {
			return conf;
		}
	}

	public static boolean isValidConf(Map<String, Object> stormConf) {
		return normalizeConf(stormConf).equals(
				normalizeConf((Map) JSONValue.parse(JSONValue
						.toJSONString(stormConf))));
	}

	public static Object getSetComponentObject(ComponentObject obj,
			URLClassLoader loader) {
		if (obj.getSetField() == ComponentObject._Fields.SERIALIZED_JAVA) {
			return Utils.deserialize(obj.get_serialized_java(), loader);
		} else if (obj.getSetField() == ComponentObject._Fields.JAVA_OBJECT) {
			return obj.get_java_object();
		} else {
			return obj.get_shell();
		}
	}

	public static <S, T> T get(Map<S, T> m, S key, T def) {
		T ret = m.get(key);
		if (ret == null) {
			ret = def;
		}
		return ret;
	}

	public static List<Object> tuple(Object... values) {
		List<Object> ret = new ArrayList<Object>();
		for (Object v : values) {
			ret.add(v);
		}
		return ret;
	}

	public static void downloadFromMaster(Map conf, String file,
			String localFile) throws IOException, TException {
		WritableByteChannel out = null;
		NimbusClient client = null;
		try {
			client = NimbusClient.getConfiguredClient(conf);
			String id = client.getClient().beginFileDownload(file);
			out = Channels.newChannel(new FileOutputStream(localFile));
			while (true) {
				ByteBuffer chunk = client.getClient().downloadChunk(id);
				int written = out.write(chunk);
				if (written == 0)
					break;
			}
		} finally {
			if (out != null)
				out.close();
			if (client != null)
				client.close();
		}
	}

	public static IFn loadClojureFn(String namespace, String name) {
		try {
			clojure.lang.Compiler.eval(RT.readString("(require '" + namespace
					+ ")"));
		} catch (Exception e) {
			// if playing from the repl and defining functions, file won't exist
		}
		return (IFn) RT.var(namespace, name).deref();
	}

	public static boolean isSystemId(String id) {
		return id.startsWith("__");
	}

	public static <K, V> Map<V, K> reverseMap(Map<K, V> map) {
		Map<V, K> ret = new HashMap<V, K>();
		for (K key : map.keySet()) {
			ret.put(map.get(key), key);
		}
		return ret;
	}

	public static ComponentCommon getComponentCommon(StormTopology topology,
			String id) {
		if (topology.get_spouts().containsKey(id)) {
			return topology.get_spouts().get(id).get_common();
		}
		if (topology.get_bolts().containsKey(id)) {
			return topology.get_bolts().get(id).get_common();
		}
		if (topology.get_state_spouts().containsKey(id)) {
			return topology.get_state_spouts().get(id).get_common();
		}
		throw new IllegalArgumentException("Could not find component with id "
				+ id);
	}

	public static Integer getInt(Object o) {
		if (o instanceof Long) {
			return ((Long) o).intValue();
		} else if (o instanceof Integer) {
			return (Integer) o;
		} else if (o instanceof Short) {
			return ((Short) o).intValue();
		} else if (o instanceof String) {
			return Integer.parseInt(((String) o));
		} else {
			throw new IllegalArgumentException("Don't know how to convert " + o
					+ " to int");
		}
	}
	
	public static Integer getInt(Object o, Integer defaultValue) {
	      if (null == o) {
	        return defaultValue;
	      }
	      
	      if(o instanceof Long) {
	          return ((Long) o ).intValue();
	      } else if (o instanceof Integer) {
	          return (Integer) o;
	      } else if (o instanceof Short) {
	          return ((Short) o).intValue();
	      } else if (o instanceof String) {
	          return Integer.parseInt(((String) o));
	      } else {
	          throw new IllegalArgumentException("Don't know how to convert " + o + " to int");
	      }
	    }

	public static long secureRandomLong() {
		return UUID.randomUUID().getLeastSignificantBits();
	}

	public static CuratorFramework newCurator(Map conf, List<String> servers,
			Object port, String root) {
		return newCurator(conf, servers, port, root, null);
	}

	public static class BoundedExponentialBackoffRetry extends
			ExponentialBackoffRetry {

		protected final int maxRetryInterval;

		public BoundedExponentialBackoffRetry(int baseSleepTimeMs,
				int maxRetries, int maxSleepTimeMs) {
			super(baseSleepTimeMs, maxRetries);
			this.maxRetryInterval = maxSleepTimeMs;
		}

		public int getMaxRetryInterval() {
			return this.maxRetryInterval;
		}

		@Override
		public int getSleepTimeMs(int count, long elapsedMs) {
			return Math.min(maxRetryInterval,
					super.getSleepTimeMs(count, elapsedMs));
		}

	}

	public static CuratorFramework newCurator(Map conf, List<String> servers,
			Object port, String root, ZookeeperAuthInfo auth) {
		List<String> serverPorts = new ArrayList<String>();
		for (String zkServer : (List<String>) servers) {
			serverPorts.add(zkServer + ":" + Utils.getInt(port));
		}
		String zkStr = StringUtils.join(serverPorts, ",") + root;
		CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory
				.builder()
				.connectString(zkStr)
				.connectionTimeoutMs(
						Utils.getInt(conf
								.get(Config.STORM_ZOOKEEPER_CONNECTION_TIMEOUT)))
				.sessionTimeoutMs(
						Utils.getInt(conf
								.get(Config.STORM_ZOOKEEPER_SESSION_TIMEOUT)))
				.retryPolicy(
						new BoundedExponentialBackoffRetry(
								Utils.getInt(conf
										.get(Config.STORM_ZOOKEEPER_RETRY_INTERVAL)),
								Utils.getInt(conf
										.get(Config.STORM_ZOOKEEPER_RETRY_TIMES)),
								Utils.getInt(conf
										.get(Config.STORM_ZOOKEEPER_RETRY_INTERVAL_CEILING))));
		if (auth != null && auth.scheme != null) {
			builder = builder.authorization(auth.scheme, auth.payload);
		}
		return builder.build();
	}

	public static CuratorFramework newCurator(Map conf, List<String> servers,
			Object port) {
		return newCurator(conf, servers, port, "");
	}

	public static CuratorFramework newCuratorStarted(Map conf,
			List<String> servers, Object port, String root) {
		CuratorFramework ret = newCurator(conf, servers, port, root);
		ret.start();
		return ret;
	}

	public static CuratorFramework newCuratorStarted(Map conf,
			List<String> servers, Object port) {
		CuratorFramework ret = newCurator(conf, servers, port);
		ret.start();
		return ret;
	}

	/**
	 * 
	 (defn integer-divided [sum num-pieces] (let [base (int (/ sum
	 * num-pieces)) num-inc (mod sum num-pieces) num-bases (- num-pieces
	 * num-inc)] (if (= num-inc 0) {base num-bases} {base num-bases (inc base)
	 * num-inc} )))
	 * 
	 * @param sum
	 * @param numPieces
	 * @return
	 */

	public static TreeMap<Integer, Integer> integerDivided(int sum,
			int numPieces) {
		int base = sum / numPieces;
		int numInc = sum % numPieces;
		int numBases = numPieces - numInc;
		TreeMap<Integer, Integer> ret = new TreeMap<Integer, Integer>();
		ret.put(base, numBases);
		if (numInc != 0) {
			ret.put(base + 1, numInc);
		}
		return ret;
	}

	public static byte[] toByteArray(ByteBuffer buffer) {
		byte[] ret = new byte[buffer.remaining()];
		buffer.get(ret, 0, ret.length);
		return ret;
	}

	public static boolean exceptionCauseIsInstanceOf(Class klass,
			Throwable throwable) {
		Throwable t = throwable;
		while (t != null) {
			if (klass.isInstance(t)) {
				return true;
			}
			t = t.getCause();
		}
		return false;
	}

	public static List<String> tokenize_path(String path) {
		String[] toks = path.split("/");
		java.util.ArrayList<String> rtn = new ArrayList<String>();
		for (String str : toks) {
			if (!str.isEmpty()) {
				rtn.add(str);
			}
		}
		return rtn;
	}

	public static String toks_to_path(List<String> toks) {
		StringBuffer buff = new StringBuffer();
		buff.append("/");
		int size = toks.size();
		for (int i = 0; i < size; i++) {
			buff.append(toks.get(i));
			if (i < (size - 1)) {
				buff.append("/");
			}

		}
		return buff.toString();
	}

	public static String normalize_path(String path) {
		String rtn = toks_to_path(tokenize_path(path));
		return rtn;
	}
	
}
