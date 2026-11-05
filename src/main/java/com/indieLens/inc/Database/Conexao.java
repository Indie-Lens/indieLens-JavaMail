package com.indieLens.inc.Database;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import io.github.cdimascio.dotenv.Dotenv;

public class Conexao {

	private DataSource conexao;
	
	public Conexao() {
		this(Dotenv.configure().ignoreIfMissing().load());
	}

	public Conexao(Dotenv env) {
		DriverManagerDataSource driver = new DriverManagerDataSource();
		
		driver.setUsername(env.get("DB_USER"));
		driver.setPassword(env.get("DB_SENHA"));
		driver.setUrl("jdbc:mysql://" + env.get("DB_HOST") + ":" + env.get("DB_PORT") + "/" + env.get("DB_NAME") + "?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC&connectTimeout=10000&socketTimeout=10000");
		driver.setDriverClassName("com.mysql.cj.jdbc.Driver");
		
		this.conexao = driver;
	}

	public Conexao(DataSource fonte) {
		this.conexao = fonte;
	}
	
	public DataSource getConexao() {
		return conexao;
	}

	public void checarConexao() throws SQLException {
		try (Connection conexaoTeste = conexao.getConnection();
				Statement comando = conexaoTeste.createStatement()) {
			comando.execute("SELECT 1");
		}
	}
}
