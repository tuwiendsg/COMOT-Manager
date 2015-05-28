package at.ac.tuwien.dsg.comot.m.core.test.utils;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.tuwien.dsg.comot.m.common.ServiceClient;
import at.ac.tuwien.dsg.comot.m.core.UtilsFile;

import com.jcraft.jsch.JSchException;

public class ManualTestScp {

	private static final Logger LOG = LoggerFactory.getLogger(ServiceClient.class);

	public static void main(String[] arg) throws IOException, JSchException {
		LOG.info("++++++SCP START");

		String pem = "src/main/resources/jcik.pem";

		String lfile = "src/main/resources/test.txt";
		String user = "ubuntu";

		String host = "128.130.172.215";
		String rfile = "/var/www/html/salsa/upload/files/juraj/test.txt";

		UtilsFile.upload(new File(lfile), host, rfile, user, new File(pem));

		LOG.info("++++++SCP END");

	}

}