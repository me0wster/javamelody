/*
 * Copyright 2008-2014 by Emeric Vernat
 *
 *     This file is part of Java Melody.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bull.javamelody;

import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpSession;

import net.bull.javamelody.SessionInformations.SessionAttribute;

/**
 * Partie du rapport html pour les sessions http.
 * @author Emeric Vernat
 */
class HtmlSessionInformationsReport extends HtmlAbstractReport {
	private static final String A_HREF_PART_SESSIONS = "<a href='?part=sessions";
	private final List<SessionInformations> sessionsInformations;
	private final HttpSession currentSession;
	private final DecimalFormat integerFormat = I18N.createIntegerFormat();
	private final DateFormat durationFormat = I18N.createDurationFormat();
	private final DateFormat expiryFormat = I18N.createDateAndTimeFormat();

	HtmlSessionInformationsReport(List<SessionInformations> sessionsInformations, Writer writer) {
		super(writer);
		this.sessionsInformations = sessionsInformations;
		this.currentSession = SessionListener.getCurrentSession();
	}

	@Override
	void toHtml() throws IOException {
		writeBackAndRefreshLinks();
		writeln("<br/>");

		assert sessionsInformations != null;
		if (sessionsInformations.isEmpty()) {
			writeln("#Aucune_session#");
			return;
		}
		writeTitle("system-users.png", getString("Sessions"));
		writeSessions(sessionsInformations);
		long totalSerializedSize = 0;
		int nbSerializableSessions = 0;
		for (final SessionInformations sessionInformations : sessionsInformations) {
			final int size = sessionInformations.getSerializedSize();
			if (size >= 0) {
				totalSerializedSize += size;
				nbSerializableSessions++;
			}
		}
		final long meanSerializedSize;
		if (nbSerializableSessions > 0) {
			meanSerializedSize = totalSerializedSize / nbSerializableSessions;
		} else {
			meanSerializedSize = -1;
		}
		writeln("<div align='right'>"
				+ getFormattedString("nb_sessions", sessionsInformations.size()) + "<br/><br/>"
				+ getFormattedString("taille_moyenne_sessions", meanSerializedSize) + "</div>");
	}

	private void writeSessions(List<SessionInformations> sessions) throws IOException {
		boolean displayUser = false;
		for (final SessionInformations sessionInformations : sessions) {
			if (sessionInformations.getRemoteUser() != null) {
				displayUser = true;
				break;
			}
		}
		final HtmlTable table = new HtmlTable();
		table.beginTable(getString("Sessions"));
		write("<th>#Session_id#</th><th class='sorttable_numeric'>#Dernier_acces#</th>");
		write("<th class='sorttable_numeric'>#Age#</th><th class='sorttable_date'>#Expiration#</th>");
		write("<th class='sorttable_numeric'>#Nb_attributs#</th><th>#Serialisable#</th><th>#Taille_serialisee#</th>");
		write("<th class='sorttable_numeric'>#Adresse_IP#</th><th>#Pays#</th>");
		if (displayUser) {
			write("<th>#Utilisateur#</th>");
		}
		write("<th class='noPrint'>#Invalider#</th>");
		for (final SessionInformations session : sessions) {
			table.nextRow();
			writeSession(session, displayUser);
		}
		table.endTable();
	}

	private void writeBackAndRefreshLinks() throws IOException {
		writeln("<div class='noPrint'>");
		writeln("<a href='javascript:history.back()'>");
		writeln("<img src='?resource=action_back.png' alt='#Retour#'/> #Retour#</a>");
		writeln("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
		writeln(A_HREF_PART_SESSIONS + "'>");
		writeln("<img src='?resource=action_refresh.png' alt='#Actualiser#'/> #Actualiser#</a>");
		if (isPdfEnabled()) {
			writeln("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
			write(A_HREF_PART_SESSIONS + "&amp;format=pdf' title='#afficher_PDF#'>");
			write("<img src='?resource=pdf.png' alt='#PDF#'/> #PDF#</a>");
		}
		writeln("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
		writeln(A_HREF_PART_SESSIONS
				+ "&amp;action=invalidate_sessions' onclick=\"javascript:return confirm('"
				+ getStringForJavascript("confirm_invalidate_sessions") + "');\">");
		writeln("<img width='16' height='16' src='?resource=user-trash.png' alt='#invalidate_sessions#' title='#invalidate_sessions#' /> #invalidate_sessions#</a>");
		writeln("</div>");
	}

	private void writeBackAndRefreshLinksForSession(String sessionId) throws IOException {
		writeln("<div class='noPrint'>");
		writeln("<a href='javascript:history.back()'><img src='?resource=action_back.png' alt='#Retour#'/> #Retour#</a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
		writeln(A_HREF_PART_SESSIONS + "&amp;sessionId=" + urlEncode(sessionId) + "'>");
		writeln("<img src='?resource=action_refresh.png' alt='#Actualiser#'/> #Actualiser#</a>");
		writeln("</div>");
	}

	private void writeSession(SessionInformations session, boolean displayUser) throws IOException {
		final String nextColumnAlignRight = "</td><td align='right'>";
		final String nextColumnAlignCenter = "</td><td align='center'>";
		write("<td><a href='?part=sessions&amp;sessionId=");
		write(urlEncode(session.getId()));
		write("'>");
		write(htmlEncodeButNotSpace(session.getId()));
		write("</a>");
		if (currentSession != null && session.getId().equals(currentSession.getId())) {
			write("<img src='?resource=bullets/blue.png' alt='#Votre_session#' title='#Votre_session#'/>");
		}
		write(nextColumnAlignRight);
		write(durationFormat.format(session.getLastAccess()));
		write(nextColumnAlignRight);
		write(durationFormat.format(session.getAge()));
		write(nextColumnAlignRight);
		write(expiryFormat.format(session.getExpirationDate()));

		write(nextColumnAlignRight);
		write(integerFormat.format(session.getAttributeCount()));
		write(nextColumnAlignCenter);
		if (session.isSerializable()) {
			write("#oui#");
		} else {
			write("<span class='severe'>#non#</span>");
		}
		write(nextColumnAlignRight);
		write(integerFormat.format(session.getSerializedSize()));
		final String nextColumn = "</td><td>";
		write(nextColumn);
		final String remoteAddr = session.getRemoteAddr();
		if (remoteAddr == null) {
			write("&nbsp;");
		} else {
			writeDirectly(htmlEncodeButNotSpace(remoteAddr));
		}
		write(nextColumnAlignCenter);
		writeCountry(session);
		if (displayUser) {
			write(nextColumn);
			final String remoteUser = session.getRemoteUser();
			if (remoteUser == null) {
				write("&nbsp;");
			} else {
				writeDirectly(htmlEncodeButNotSpace(remoteUser));
			}
		}
		write("</td><td align='center' class='noPrint'>");
		write(A_HREF_PART_SESSIONS);
		write("&amp;action=invalidate_session&amp;sessionId=");
		write(urlEncode(session.getId()));
		write("' onclick=\"javascript:return confirm('"
				+ getStringForJavascript("confirm_invalidate_session") + "');\">");
		write("<img width='16' height='16' src='?resource=user-trash.png' alt='#invalidate_session#' title='#invalidate_session#' />");
		write("</a>");
		write("</td>");
	}

	private void writeCountry(SessionInformations session) throws IOException {
		final String country = session.getCountry();
		if (country == null) {
			write("&nbsp;");
		} else {
			final String fileName = "flags/" + country + ".gif";
			if (getClass().getResource(Parameters.getResourcePath(fileName)) == null) {
				write(htmlEncodeButNotSpace(country));
			} else {
				write("<img src='?resource=");
				write(urlEncode(fileName));
				write("' alt='");
				write(urlEncode(country));
				write("' title='");
				write(urlEncode(session.getCountryDisplay()));
				write("' />");
			}
		}
	}

	void writeSessionDetails(String sessionId, SessionInformations sessionInformations)
			throws IOException {
		writeBackAndRefreshLinksForSession(sessionId);
		writeln("<br/>");

		if (sessionInformations == null) {
			writeln(getFormattedString("session_invalidee", htmlEncodeButNotSpace(sessionId)));
			return;
		}
		writeTitle("system-users.png",
				getFormattedString("Details_session", htmlEncodeButNotSpace(sessionId)));
		writeSessions(Collections.singletonList(sessionInformations));

		writeln("<br/><b>#Attributs#</b>");
		writeSessionAttributes(sessionInformations);
	}

	private void writeSessionAttributes(SessionInformations sessionInformations) throws IOException {
		final HtmlTable table = new HtmlTable();
		table.beginTable(getString("Attributs"));
		write("<th>#Nom#</th><th>Type</th><th>#Serialisable#</th><th>#Taille_serialisee#</th><th>#Contenu#</th>");
		for (final SessionAttribute sessionAttribute : sessionInformations.getAttributes()) {
			table.nextRow();
			writeAttribute(sessionAttribute);
		}
		table.endTable();
	}

	private void writeAttribute(SessionAttribute sessionAttribute) throws IOException {
		write("<td>");
		writeDirectly(htmlEncodeButNotSpace(sessionAttribute.getName()));
		write("</td><td>");
		write(String.valueOf(sessionAttribute.getType()));
		write("</td><td align='center'>");
		if (sessionAttribute.isSerializable()) {
			write("#oui#");
		} else {
			write("<span class='severe'>#non#</span>");
		}
		write("</td><td align='right'>");
		write(integerFormat.format(sessionAttribute.getSerializedSize()));
		write("</td><td>");
		writeDirectly(htmlEncodeButNotSpace(String.valueOf(sessionAttribute.getContent())));
		write("</td>");
	}
}
