package com.k1ngtle.vsia.client.screen;

import com.k1ngtle.vsia.signality.internet.server.ServerRackNetwork;
import com.k1ngtle.vsia.signality.internet.server.ServerRackNetwork.OpenRackPacket;
import com.k1ngtle.vsia.signality.internet.server.ServerRackService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

public final class ServerRackScreen extends Screen {
    private enum Tab { PHYSICAL, CONFIG, SERVICES, DESKTOP, PROGRAMMING, ATTRIBUTES }
    private Tab currentTab = Tab.PHYSICAL;
    private String selectedConfigItem = "Settings";
    private String selectedServiceItem = "HTTP";
    private String desktopPage = "";

    private final OpenRackPacket state;
    private final long openedAtMillis = System.currentTimeMillis();

    private float configScrollOffset = 0.0f;
    private int maxConfigScrollLines = 0;
    private final List<String[]> configTreeItems = new ArrayList<>();

    private float servicesScrollOffset = 0.0f;
    private int maxServicesScrollLines = 0;
    private final List<String[]> servicesTreeItems = new ArrayList<>();

    private boolean isUpdatingVisibility = false;
    private int imageWidth = 720;
    private int imageHeight = 460;

    // Fields
    private EditBox name, ip, subnet, gateway, dns, ipv6, prefix6, gateway6, dns6, clockOffset;
    private EditBox dnsName, dnsDetail, dnsTtl;
    private EditBox poolName, poolStart, poolEnd, poolPrefix, poolGateway, poolDns, poolLease, poolExclusions;
    private EditBox mailDomainField, mailUserField, mailPasswordField, mailQuotaField;
    private EditBox ntpStratumField, ntpPollField, ntpSourceField, ntpDriftField;
    private EditBox syslogMinField, syslogFacilityField, syslogSeverityField, syslogMessageField;
    private EditBox aaaUserField, aaaPasswordField, aaaPrivilegeField, aaaServiceField;
    private EditBox radiusNameField, radiusAddressField, radiusSecretField, radiusUserField, radiusPasswordField, radiusPrivilegeField;
    private EditBox iotIdField, iotNameField, iotTypeField, iotValueField;
    private EditBox vmNameField, vmOsField, vmCpuField, vmMemoryField, vmStorageField;
    private EditBox prpPeerField;
    private EditBox httpFileNameField, httpContentField, httpPortField, httpsPortField;
    private EditBox transferFileNameField, transferContentField, transferPortField, transferUserField, transferPasswordField;
    private EditBox mailClientAddressField, mailClientPasswordField, mailClientToField, mailClientSubjectField, mailClientBodyField;
    private EditBox dnsClientNameField, dnsClientServerField;
    private EditBox toolInput, programInput;
    private EditBox textFileNameField;
    private MultiLineEditBox textFileEditor;
    private EditBox ftpClientServerField, ftpClientUserField, ftpClientPasswordField, ftpClientRemoteField, ftpClientLocalField;
    private EditBox tftpClientServerField, tftpClientRemoteField, tftpClientLocalField;
    private MultiLineEditBox httpMultiLineEditor;

    // State Variables
    private String desktopOutput = "Ready.";
    private String dnsClientType = "A";
    private String textFileData = "", textFileStatus = "Press Refresh to load documents.", textFileName = "", textFileContent = "";
    private int textFileScroll;
    private String ftpClientServer = "192.168.1.2", ftpClientUser = "", ftpClientPassword = "", ftpClientRemoteFiles = "", ftpClientLocalFiles = "", ftpClientStatus = "Enter credentials.";
    private boolean ftpClientConnected;
    private int ftpClientRemoteScroll, ftpClientLocalScroll;
    private String tftpClientServer = "192.168.1.2", tftpClientRemoteFiles = "", tftpClientLocalFiles = "", tftpClientStatus = "Enter TFTP address.";
    private boolean tftpClientConnected;
    private int tftpClientRemoteScroll, tftpClientLocalScroll;
    private final List<String> browserHistory = new ArrayList<>();
    private final List<String[]> browserLinks = new ArrayList<>();
    private final List<String[]> browserActions = new ArrayList<>();
    private final List<String[]> browserFormFields = new ArrayList<>();
    private final List<EditBox> browserFormWidgets = new ArrayList<>();
    private String browserFormAction = "";
    private int browserHistoryIndex = -1, browserStatus = 0;
    private String browserUrl = "http://192.168.1.2/", browserTitle = "VSIA Browser", browserPageText = "Enter an address and press Go.";
    private String browserNotice = "";
    private int browserBackground = 0xFFF4F4F4, browserForeground = 0xFF202020;
    private String programOutput = "Ready. Separate commands with semicolons. Type help for commands.";

    private boolean dhcp, http, dnsService, dhcpService, mail, automatic6;
    private String ptpMode, ptpProfile;
    private long serviceMask;
    private String dnsRecordData, dnsRecordType = "A", dnsStatus = "Ready.";
    private String dhcp4Data, dhcp6Data, dhcpStatus = "Ready.";
    private boolean https = true, pop3 = true, httpReadable = true, httpWritable = true;
    private String httpFiles = "", httpStatus = "Press Refresh to load hosted files.";
    private int configuredHttpPort = 80, configuredHttpsPort = 443;
    private boolean httpEditorOpen;
    private int httpFileScroll;
    private String transferFiles = "", transferUsers = "", transferStatus = "Press Refresh to load transfer data.";
    private int configuredFtpPort = 21, configuredTftpPort = 69, transferFileScroll;
    private boolean transferReadable = true, transferWritable = true, transferEditorOpen;
    private final Map<String, Integer> serviceScrolls = new HashMap<>();
    private String serviceStatus = "Ready.";
    private boolean ntpServer, ntpClient;
    private String ntpStatus;
    private long ntpDeviceTime, lastNtpSync;
    private String syslogData = "", syslogStatus = "Press Refresh to load entries.";
    private boolean syslogAcceptRemote = true;
    private String aaaUsers = "", aaaAccounting = "", aaaStatus = "Press Refresh to load AAA data.";
    private boolean aaaUserEnabled = true;
    private String radiusClients = "", radiusEvents = "", radiusStatus = "Press Refresh to load RADIUS data.";
    private boolean radiusClientEnabled = true;
    private String iotDevices = "", iotStatus = "Press Refresh to load IoT devices.";
    private String virtualMachines = "", vmStatus = "Press Refresh to load virtual machines.";
    private String prpStatus = "Press Refresh to load PRP supervision.", prpData = "";
    private boolean prpEnabled, prpLaneA = true, prpLaneB = true;
    private boolean mailClientLoggedIn, mailClientCompose, mailClientReading;
    private String mailClientAddress = "", mailClientPassword = "", mailClientFolder = "INBOX", mailClientData = "", mailClientStatus = "Enter your mailbox and password.", mailClientMessageId = "", mailClientFrom = "", mailClientTo = "", mailClientSubject = "", mailClientBody = "";
    private long mailClientSentAt;

    public ServerRackScreen(OpenRackPacket state) {
        super(Component.literal("VSIA Server Rack"));
        this.state = state;
        dhcp = state.dhcp(); http = state.http(); dnsService = state.dnsService();
        dhcpService = state.dhcpService(); mail = state.mail();
        automatic6 = state.automatic6(); ptpMode = state.ptpMode(); ptpProfile = state.ptpProfile();
        serviceMask = state.serviceMask();
        dnsRecordData = state.dnsRecordData();
        dhcp4Data = state.dhcp4Data(); dhcp6Data = state.dhcp6Data();
        ntpServer = state.ntpServer(); ntpClient = state.ntpClient(); ntpStatus = state.clockStatus(); ntpDeviceTime = state.deviceTime(); lastNtpSync = state.lastNtpSync();
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        this.browserFormWidgets.clear();

        this.configTreeItems.clear();
        this.configTreeItems.add(new String[]{"GLOBAL", "0xDDDDDD", "0", "header"});
        this.configTreeItems.add(new String[]{"    Settings", "0xFFFFFF", "1", "item"});
        this.configTreeItems.add(new String[]{"    Clock & PTP", "0xAAAAAA", "0", "item"});
        this.configTreeItems.add(new String[]{"", "0x000000", "0", "empty"});
        this.configTreeItems.add(new String[]{"INTERFACE", "0xDDDDDD", "0", "header"});
        this.configTreeItems.add(new String[]{"    FastEthernet0", "0xAAAAAA", "0", "item"});

        this.servicesTreeItems.clear();
        this.servicesTreeItems.add(new String[]{"SERVICES", "0xDDDDDD", "0", "header"});
        String[] srvList = {"HTTP", "FTP", "TFTP", "DNS", "DHCP", "DHCPv6", "EMAIL", "NTP", "SYSLOG", "AAA", "RADIUS EAP", "IoT", "VM Management", "PRP"};
        for (String s : srvList) this.servicesTreeItems.add(new String[]{"    " + s, "0xAAAAAA", "0", "item"});

        name = field(state.displayName(), 32);
        ip = field(state.ip(), 15); subnet = field(state.subnet(), 15); gateway = field(state.gateway(), 15); dns = field(state.dns(), 15);
        ipv6 = field(state.ipv6(), 45); prefix6 = field(Integer.toString(state.ipv6Prefix()), 3); gateway6 = field(state.gateway6(), 45); dns6 = field(state.dns6(), 45);
        clockOffset = field(Long.toString(state.clockOffset() / 1000L), 8);
        dnsName = field("", 253); dnsDetail = field("", 253); dnsTtl = field("300", 5);
        poolName = field("LAN", 32); poolStart = field("192.168.1.100", 45); poolEnd = field("192.168.1.254", 45); poolPrefix = field(state.subnet(), 15); poolGateway = field(state.gateway(), 45); poolDns = field(state.dns(), 45); poolLease = field("3600", 7); poolExclusions = field("", 1024);
        mailDomainField = field("vsia-net.com", 253); mailUserField = field("", 32); mailPasswordField = field("", 64); mailQuotaField = field("100", 4);
        ntpStratumField = field(Integer.toString(state.ntpStratum()), 2); ntpPollField = field(Integer.toString(state.ntpPoll()), 4); ntpSourceField = field(state.ntpSource(), 45); ntpDriftField = field(Integer.toString(state.clockDrift()), 5);
        syslogMinField = field("7", 1); syslogFacilityField = field("LOCAL0", 16); syslogSeverityField = field("6", 1); syslogMessageField = field("Syslog test message", 512);
        aaaUserField = field("", 32); aaaPasswordField = field("", 64); aaaPrivilegeField = field("1", 2); aaaServiceField = field("LOGIN", 32);
        radiusNameField = field("Campus NAS", 32); radiusAddressField = field("192.168.1.10", 15); radiusSecretField = field("vsia-radius", 64); radiusUserField = field("admin", 32); radiusPasswordField = field("admin", 64); radiusPrivilegeField = field("1", 2);
        iotIdField = field("sensor-01", 32); iotNameField = field("Temperature Sensor", 32); iotTypeField = field("SENSOR", 24); iotValueField = field("temperature=22.5C", 128);
        vmNameField = field("web-vm-01", 32); vmOsField = field("VSIA Linux", 32); vmCpuField = field("2", 2); vmMemoryField = field("4096", 5); vmStorageField = field("64", 4);
        prpPeerField = field("192.168.1.3", 15);
        httpFileNameField = field("", 64); httpContentField = field("", 32768); httpPortField = field(Integer.toString(configuredHttpPort), 5); httpsPortField = field(Integer.toString(configuredHttpsPort), 5);
        transferFileNameField = field("new-file.txt", 64); transferContentField = field("", 32768); transferPortField = field(Integer.toString(configuredFtpPort), 5); transferUserField = field("", 32); transferPasswordField = field("", 64);
        mailClientAddressField = field(mailClientAddress, 286); mailClientPasswordField = field("", 64); mailClientToField = field(mailClientTo, 286); mailClientSubjectField = field(mailClientSubject, 128); mailClientBodyField = field(mailClientBody, 8192);
        dnsClientNameField = field("", 253); dnsClientServerField = field(state.dns(), 45);
        toolInput = field("", 256); programInput = field("hostname Server0; show config", 16384);
        textFileNameField = field(textFileName, 64); ftpClientServerField = field(ftpClientServer, 15); ftpClientUserField = field(ftpClientUser, 32); ftpClientPasswordField = field(ftpClientPassword, 64); ftpClientRemoteField = field("", 64); ftpClientLocalField = field("", 64);
        tftpClientServerField = field(tftpClientServer, 15); tftpClientRemoteField = field("", 64); tftpClientLocalField = field("", 64);

        textFileEditor = new MultiLineEditBox(font, 0, 0, 100, 100, Component.literal("Enter text"), Component.literal("VSIA Editor")); textFileEditor.setCharacterLimit(32768); addRenderableWidget(textFileEditor);
        httpMultiLineEditor = new MultiLineEditBox(font, 0, 0, 100, 100, Component.literal("Enter HTML"), Component.literal("Web Editor")); httpMultiLineEditor.setCharacterLimit(32768); addRenderableWidget(httpMultiLineEditor);

        updateVisibility();
    }

    private EditBox field(String val, int max) {
        EditBox e = new EditBox(font, 0, 0, 100, 12, Component.empty()); e.setMaxLength(max); e.setValue(val); e.setBordered(false); e.setTextColor(0xFFFFFF); addRenderableWidget(e); e.setVisible(false); return e;
    }

    public EditBox hiddenHttpContent(String value) {
        EditBox box = new EditBox(font, -1000, -1000, 60, 18, Component.empty()); box.setMaxLength(32768); box.setValue(value); box.setVisible(false); return box;
    }

    private void updateVisibility() {
        this.isUpdatingVisibility = true;
        for (var child : this.children()) {
            if (child instanceof EditBox e) e.setVisible(false);
            if (child instanceof MultiLineEditBox m) m.visible = false;
        }

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int contentX = x + 180;
        int inputX = contentX + 140;
        int servContentX = x + 180;
        int rightEnd = x + this.imageWidth - 16;
        int fullWidth = rightEnd - servContentX;

        int offset = 55;
        int col0 = servContentX + 5;
        int col1 = servContentX + 175;
        int col2 = servContentX + 345;

        if (currentTab == Tab.CONFIG) {
            if (selectedConfigItem.equals("Settings")) {
                name.setPosition(inputX, y + offset + 0*22 + 5); name.setWidth(200); name.setVisible(true);
                gateway.setPosition(inputX, y + offset + 2*22 + 5); gateway.setWidth(150); gateway.setVisible(true);
                dns.setPosition(inputX, y + offset + 3*22 + 5); dns.setWidth(150); dns.setVisible(true);
                gateway6.setPosition(inputX, y + offset + 5*22 + 5); gateway6.setWidth(250); gateway6.setVisible(true);
                dns6.setPosition(inputX, y + offset + 6*22 + 5); dns6.setWidth(250); dns6.setVisible(true);
            } else if (selectedConfigItem.equals("FastEthernet0")) {
                ip.setPosition(inputX, y + offset + 1*22 + 5); ip.setWidth(150); ip.setVisible(true);
                subnet.setPosition(inputX, y + offset + 2*22 + 5); subnet.setWidth(150); subnet.setVisible(true);
                ipv6.setPosition(inputX, y + offset + 3*22 + 5); ipv6.setWidth(250); ipv6.setVisible(true);
                prefix6.setPosition(inputX, y + offset + 4*22 + 5); prefix6.setWidth(50); prefix6.setVisible(true);
            } else if (selectedConfigItem.equals("Clock & PTP")) {
                clockOffset.setPosition(inputX, y + offset + 0*22 + 5); clockOffset.setWidth(100); clockOffset.setVisible(true);
            }
        } else if (currentTab == Tab.SERVICES) {
            if (selectedServiceItem.equals("HTTP")) {
                if (httpEditorOpen) {
                    httpFileNameField.setPosition(servContentX, y + 63); httpFileNameField.setWidth(fullWidth); httpFileNameField.setVisible(true);
                    httpMultiLineEditor.setX(servContentX); httpMultiLineEditor.setY(y + 85); httpMultiLineEditor.setWidth(fullWidth); httpMultiLineEditor.setHeight(this.imageHeight - 145); httpMultiLineEditor.visible = true;
                } else {
                    httpPortField.setPosition(inputX, y + offset + 1*22 + 5); httpPortField.setWidth(60); httpPortField.setVisible(true);
                    httpsPortField.setPosition(inputX + 150, y + offset + 1*22 + 5); httpsPortField.setWidth(60); httpsPortField.setVisible(true);
                }
            } else if (selectedServiceItem.equals("FTP") || selectedServiceItem.equals("TFTP")) {
                if (transferEditorOpen) {
                    transferFileNameField.setPosition(servContentX, y + 63); transferFileNameField.setWidth(fullWidth); transferFileNameField.setVisible(true);
                    transferContentField.setPosition(servContentX, y + 85); transferContentField.setWidth(fullWidth); transferContentField.setVisible(true);
                } else {
                    transferPortField.setPosition(inputX, y + offset + 0*22 + 5); transferPortField.setWidth(60); transferPortField.setVisible(true);
                    if (selectedServiceItem.equals("FTP")) {
                        transferUserField.setPosition(col0, y + offset + 22 + 0*36 + 18); transferUserField.setWidth(160); transferUserField.setVisible(true);
                        transferPasswordField.setPosition(col1, y + offset + 22 + 0*36 + 18); transferPasswordField.setWidth(160); transferPasswordField.setVisible(true);
                    }
                }
            } else if (selectedServiceItem.equals("DNS")) {
                dnsName.setPosition(inputX, y + offset + 1*22 + 5); dnsName.setWidth(200); dnsName.setVisible(true);
                dnsDetail.setPosition(inputX, y + offset + 2*22 + 5); dnsDetail.setWidth(200); dnsDetail.setVisible(true);
                dnsTtl.setPosition(inputX, y + offset + 3*22 + 5); dnsTtl.setWidth(60); dnsTtl.setVisible(true);
            } else if (selectedServiceItem.equals("DHCP") || selectedServiceItem.equals("DHCPv6")) {
                int colW = 160;
                poolName.setPosition(col0, y + offset + 0*36 + 18); poolName.setWidth(colW); poolName.setVisible(true);
                poolStart.setPosition(col1, y + offset + 0*36 + 18); poolStart.setWidth(colW); poolStart.setVisible(true);
                poolEnd.setPosition(col2, y + offset + 0*36 + 18); poolEnd.setWidth(colW); poolEnd.setVisible(true);
                poolPrefix.setPosition(col0, y + offset + 1*36 + 18); poolPrefix.setWidth(colW); poolPrefix.setVisible(true);
                poolGateway.setPosition(col1, y + offset + 1*36 + 18); poolGateway.setWidth(colW); poolGateway.setVisible(true);
                poolDns.setPosition(col2, y + offset + 1*36 + 18); poolDns.setWidth(colW); poolDns.setVisible(true);
                poolLease.setPosition(col0, y + offset + 2*36 + 18); poolLease.setWidth(colW); poolLease.setVisible(true);
                poolExclusions.setPosition(col1, y + offset + 2*36 + 18); poolExclusions.setWidth(fullWidth - (col1 - servContentX) - 5); poolExclusions.setVisible(true);
            } else if (selectedServiceItem.equals("EMAIL")) {
                mailDomainField.setPosition(inputX, y + offset + 1*22 + 5); mailDomainField.setWidth(180); mailDomainField.setVisible(true);
                int gridOffset = offset + 2*22;
                mailUserField.setPosition(col0, y + gridOffset + 0*36 + 18); mailUserField.setWidth(160); mailUserField.setVisible(true);
                mailPasswordField.setPosition(col1, y + gridOffset + 0*36 + 18); mailPasswordField.setWidth(160); mailPasswordField.setVisible(true);
                mailQuotaField.setPosition(col2, y + gridOffset + 0*36 + 18); mailQuotaField.setWidth(60); mailQuotaField.setVisible(true);
            } else if (selectedServiceItem.equals("NTP")) {
                int colW = 160;
                ntpStratumField.setPosition(col0, y + offset + 1*36 + 18); ntpStratumField.setWidth(colW); ntpStratumField.setVisible(true);
                ntpPollField.setPosition(col1, y + offset + 1*36 + 18); ntpPollField.setWidth(colW); ntpPollField.setVisible(true);
                ntpSourceField.setPosition(col0, y + offset + 2*36 + 18); ntpSourceField.setWidth(colW); ntpSourceField.setVisible(true);
                ntpDriftField.setPosition(col1, y + offset + 2*36 + 18); ntpDriftField.setWidth(colW); ntpDriftField.setVisible(true);
            } else if (selectedServiceItem.equals("SYSLOG")) {
                int colW = 160;
                syslogMinField.setPosition(col0, y + offset + 0*36 + 18); syslogMinField.setWidth(colW); syslogMinField.setVisible(true);
                syslogFacilityField.setPosition(col0, y + offset + 1*36 + 18); syslogFacilityField.setWidth(colW); syslogFacilityField.setVisible(true);
                syslogSeverityField.setPosition(col1, y + offset + 1*36 + 18); syslogSeverityField.setWidth(colW); syslogSeverityField.setVisible(true);
                syslogMessageField.setPosition(col0, y + offset + 2*36 + 18); syslogMessageField.setWidth(fullWidth - 10); syslogMessageField.setVisible(true);
            } else if (selectedServiceItem.equals("AAA")) {
                int colW = 160;
                aaaUserField.setPosition(col0, y + offset + 0*36 + 18); aaaUserField.setWidth(colW); aaaUserField.setVisible(true);
                aaaPasswordField.setPosition(col1, y + offset + 0*36 + 18); aaaPasswordField.setWidth(colW); aaaPasswordField.setVisible(true);
                aaaPrivilegeField.setPosition(col0, y + offset + 1*36 + 18); aaaPrivilegeField.setWidth(colW); aaaPrivilegeField.setVisible(true);
                aaaServiceField.setPosition(col1, y + offset + 1*36 + 18); aaaServiceField.setWidth(colW); aaaServiceField.setVisible(true);
            } else if (selectedServiceItem.equals("RADIUS EAP")) {
                int colW = 160;
                radiusNameField.setPosition(col0, y + offset + 0*36 + 18); radiusNameField.setWidth(colW); radiusNameField.setVisible(true);
                radiusAddressField.setPosition(col1, y + offset + 0*36 + 18); radiusAddressField.setWidth(colW); radiusAddressField.setVisible(true);
                radiusSecretField.setPosition(col2, y + offset + 0*36 + 18); radiusSecretField.setWidth(colW); radiusSecretField.setVisible(true);
                radiusUserField.setPosition(col0, y + offset + 1*36 + 18); radiusUserField.setWidth(colW); radiusUserField.setVisible(true);
                radiusPasswordField.setPosition(col1, y + offset + 1*36 + 18); radiusPasswordField.setWidth(colW); radiusPasswordField.setVisible(true);
                radiusPrivilegeField.setPosition(col2, y + offset + 1*36 + 18); radiusPrivilegeField.setWidth(colW); radiusPrivilegeField.setVisible(true);
            } else if (selectedServiceItem.equals("IoT")) {
                int colW = 160;
                iotIdField.setPosition(col0, y + offset + 0*36 + 18); iotIdField.setWidth(colW); iotIdField.setVisible(true);
                iotNameField.setPosition(col1, y + offset + 0*36 + 18); iotNameField.setWidth(colW); iotNameField.setVisible(true);
                iotTypeField.setPosition(col2, y + offset + 0*36 + 18); iotTypeField.setWidth(colW); iotTypeField.setVisible(true);
                iotValueField.setPosition(col0, y + offset + 1*36 + 18); iotValueField.setWidth(fullWidth - 10); iotValueField.setVisible(true);
            } else if (selectedServiceItem.equals("VM Management")) {
                int colW = 160;
                vmNameField.setPosition(col0, y + offset + 0*36 + 18); vmNameField.setWidth(colW); vmNameField.setVisible(true);
                vmOsField.setPosition(col1, y + offset + 0*36 + 18); vmOsField.setWidth(colW*2 + 10); vmOsField.setVisible(true);
                vmCpuField.setPosition(col0, y + offset + 1*36 + 18); vmCpuField.setWidth(colW); vmCpuField.setVisible(true);
                vmMemoryField.setPosition(col1, y + offset + 1*36 + 18); vmMemoryField.setWidth(colW); vmMemoryField.setVisible(true);
                vmStorageField.setPosition(col2, y + offset + 1*36 + 18); vmStorageField.setWidth(colW); vmStorageField.setVisible(true);
            } else if (selectedServiceItem.equals("PRP")) {
                prpPeerField.setPosition(inputX, y + offset + 3*22 + 5); prpPeerField.setWidth(200); prpPeerField.setVisible(true);
            }
        } else if (currentTab == Tab.DESKTOP && !desktopPage.isEmpty()) {
            if (desktopPage.equals("Web Browser")) {
                toolInput.setPosition(x + 100, y + 60); toolInput.setWidth(this.imageWidth - 170); toolInput.setVisible(true);
                int formWidth = Math.max(100, (this.imageWidth - 200) / Math.max(1, Math.min(3, browserFormFields.size())));
                for (int i = 0; i < Math.min(browserFormFields.size(), 3) && i < browserFormWidgets.size(); i++) {
                    EditBox w = browserFormWidgets.get(i);
                    w.setPosition(x + 35 + i * formWidth, y + this.imageHeight - 100); w.setWidth(formWidth - 10); w.setVisible(true);
                }
            } else if (desktopPage.equals("Terminal") || desktopPage.equals("Command Prompt") || desktopPage.equals("Ping") || desktopPage.equals("DNS Lookup")) {
                toolInput.setPosition(x + 100, y + 60); toolInput.setWidth(this.imageWidth - 170); toolInput.setVisible(true);
                if (desktopPage.equals("DNS Lookup")) {
                    dnsClientNameField.setPosition(x + 100, y + 90); dnsClientNameField.setWidth(this.imageWidth - 170); dnsClientNameField.setVisible(true);
                    dnsClientServerField.setPosition(x + 100, y + 120); dnsClientServerField.setWidth(this.imageWidth - 170); dnsClientServerField.setVisible(true);
                    toolInput.setVisible(false);
                }
            } else if (desktopPage.equals("Text Editor")) {
                textFileNameField.setPosition(x + 200, y + 60); textFileNameField.setWidth(this.imageWidth - 220); textFileNameField.setVisible(true);
                textFileEditor.setX(x + 200); textFileEditor.setY(y + 85); textFileEditor.setWidth(this.imageWidth - 220); textFileEditor.setHeight(this.imageHeight - 135); textFileEditor.visible = true;
            } else if (desktopPage.equals("FTP Client")) {
                ftpClientServerField.setPosition(x + 80, y + 60); ftpClientServerField.setWidth(120); ftpClientServerField.setVisible(true);
                ftpClientUserField.setPosition(x + 260, y + 60); ftpClientUserField.setWidth(100); ftpClientUserField.setVisible(true);
                ftpClientPasswordField.setPosition(x + 430, y + 60); ftpClientPasswordField.setWidth(100); ftpClientPasswordField.setVisible(true);
                ftpClientRemoteField.setPosition(x + 80, y + this.imageHeight - 100); ftpClientRemoteField.setWidth(200); ftpClientRemoteField.setVisible(true);
                ftpClientLocalField.setPosition(x + 400, y + this.imageHeight - 100); ftpClientLocalField.setWidth(200); ftpClientLocalField.setVisible(true);
            } else if (desktopPage.equals("TFTP Client")) {
                tftpClientServerField.setPosition(x + 80, y + 60); tftpClientServerField.setWidth(120); tftpClientServerField.setVisible(true);
                tftpClientRemoteField.setPosition(x + 80, y + this.imageHeight - 100); tftpClientRemoteField.setWidth(200); tftpClientRemoteField.setVisible(true);
                tftpClientLocalField.setPosition(x + 400, y + this.imageHeight - 100); tftpClientLocalField.setWidth(200); tftpClientLocalField.setVisible(true);
            } else if (desktopPage.equals("Email")) {
                if (!mailClientLoggedIn) {
                    mailClientAddressField.setPosition(x + 250, y + 150); mailClientAddressField.setWidth(200); mailClientAddressField.setVisible(true);
                    mailClientPasswordField.setPosition(x + 250, y + 180); mailClientPasswordField.setWidth(200); mailClientPasswordField.setVisible(true);
                } else if (mailClientCompose) {
                    mailClientToField.setPosition(x + 100, y + 100); mailClientToField.setWidth(this.imageWidth - 120); mailClientToField.setVisible(true);
                    mailClientSubjectField.setPosition(x + 100, y + 130); mailClientSubjectField.setWidth(this.imageWidth - 120); mailClientSubjectField.setVisible(true);
                    mailClientBodyField.setPosition(x + 100, y + 160); mailClientBodyField.setWidth(this.imageWidth - 120); mailClientBodyField.setVisible(true);
                }
            } else if (desktopPage.equals("IP Configuration")) {
                int ipX = x + 200; int w = 300;
                ip.setPosition(ipX, y + 100); ip.setWidth(w); ip.setVisible(true);
                subnet.setPosition(ipX, y + 120); subnet.setWidth(w); subnet.setVisible(true);
                gateway.setPosition(ipX, y + 140); gateway.setWidth(w); gateway.setVisible(true);
                dns.setPosition(ipX, y + 160); dns.setWidth(w); dns.setVisible(true);
                ipv6.setPosition(ipX, y + 210); ipv6.setWidth(w); ipv6.setVisible(true);
                prefix6.setPosition(ipX, y + 230); prefix6.setWidth(50); prefix6.setVisible(true);
                gateway6.setPosition(ipX, y + 250); gateway6.setWidth(w); gateway6.setVisible(true);
                dns6.setPosition(ipX, y + 270); dns6.setWidth(w); dns6.setVisible(true);
            }
        } else if (currentTab == Tab.PROGRAMMING) {
            programInput.setPosition(x + 20, y + 80); programInput.setWidth(this.imageWidth - 40); programInput.setVisible(true);
        }

        this.isUpdatingVisibility = false;
    }

    private boolean clickRect(double mx, double my, int rx, int ry, int rw, int rh) {
        return mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        if (mouseY >= y + 10 && mouseY <= y + 30) {
            for (int i = 0; i < Tab.values().length; i++) {
                int tabX = x + 10 + (i * 82);
                if (mouseX >= tabX && mouseX <= tabX + 80) {
                    this.currentTab = Tab.values()[i];
                    this.desktopPage = "";
                    updateVisibility();
                    return true;
                }
            }
        }

        if (this.currentTab == Tab.CONFIG) {
            int sbWidth = 160;
            int listY = y + 31;
            int listHeight = this.imageHeight - 31;
            if (mouseX >= x && mouseX <= x + sbWidth && mouseY >= listY && mouseY <= listY + listHeight) {
                int visibleItemIndex = (int) ((mouseY - listY) / 15);
                int actualIndex = (int) (this.configScrollOffset * this.maxConfigScrollLines) + visibleItemIndex;
                if (actualIndex >= 0 && actualIndex < this.configTreeItems.size()) {
                    String[] item = this.configTreeItems.get(actualIndex);
                    if (item[3].equals("item")) {
                        for (String[] i : this.configTreeItems) if (i[3].equals("item")) { i[2] = "0"; i[1] = "0xAAAAAA"; }
                        item[2] = "1"; item[1] = "0xFFFFFF";
                        this.selectedConfigItem = item[0].trim();
                        updateVisibility(); return true;
                    }
                }
            }

            int cx = x + 180;
            int inputX = cx + 140;
            int offset = 55;
            if (selectedConfigItem.equals("Settings")) {
                if (clickRect(mouseX, mouseY, inputX, y + offset + 1*22 + 6, 15, 10)) { dhcp = !dhcp; updateVisibility(); return true; }
                if (clickRect(mouseX, mouseY, inputX + 60, y + offset + 1*22 + 6, 15, 10)) { dhcp = !dhcp; updateVisibility(); return true; }
                if (clickRect(mouseX, mouseY, inputX, y + offset + 4*22 + 6, 15, 10)) { automatic6 = !automatic6; updateVisibility(); return true; }
                if (clickRect(mouseX, mouseY, inputX + 60, y + offset + 4*22 + 6, 15, 10)) { automatic6 = !automatic6; updateVisibility(); return true; }
            } else if (selectedConfigItem.equals("FastEthernet0")) {
                if (clickRect(mouseX, mouseY, inputX, y + offset + 0*22 + 6, 15, 10)) { return true; }
            } else if (selectedConfigItem.equals("Clock & PTP")) {
                if (clickRect(mouseX, mouseY, inputX, y + offset + 1*22 + 3, 100, 16)) { ptpMode = ptpMode.equals("DISABLED")?"GRANDMASTER":ptpMode.equals("GRANDMASTER")?"CLIENT":"DISABLED"; updateVisibility(); return true; }
                if (clickRect(mouseX, mouseY, inputX, y + offset + 2*22 + 3, 100, 16)) { ptpProfile = ptpProfile.equals("POWER")?"TELECOM":ptpProfile.equals("TELECOM")?"DEFAULT":"POWER"; updateVisibility(); return true; }
            }
            if (clickRect(mouseX, mouseY, x + this.imageWidth - 160, y + this.imageHeight - 25, 140, 18)) { save(); return true; }
        }

        if (this.currentTab == Tab.SERVICES) {
            int sbWidth = 160;
            int listY = y + 31;
            int listHeight = this.imageHeight - 31;
            if (mouseX >= x && mouseX <= x + sbWidth && mouseY >= listY && mouseY <= listY + listHeight) {
                int visibleItemIndex = (int) ((mouseY - listY) / 15);
                int actualIndex = (int) (this.servicesScrollOffset * this.maxServicesScrollLines) + visibleItemIndex;
                if (actualIndex >= 0 && actualIndex < this.servicesTreeItems.size()) {
                    String[] item = this.servicesTreeItems.get(actualIndex);
                    if (item[3].equals("item")) {
                        for (String[] i : this.servicesTreeItems) if (i[3].equals("item")) { i[2] = "0"; i[1] = "0xAAAAAA"; }
                        item[2] = "1"; item[1] = "0xFFFFFF";
                        this.selectedServiceItem = item[0].trim();
                        updateVisibility(); return true;
                    }
                }
            }

            int cx = x + 180;
            int inputX = cx + 140;
            int offset = 55;
            int col0 = cx + 5;
            int col1 = cx + 175;
            int col2 = cx + 345;

            if (selectedServiceItem.equals("HTTP")) {
                if (httpEditorOpen) {
                    if (clickRect(mouseX, mouseY, cx, y + this.imageHeight - 25, 80, 16)) { httpContentField = hiddenHttpContent(httpMultiLineEditor.getValue()); httpEditorOpen = false; updateVisibility(); return true; }
                    if (clickRect(mouseX, mouseY, cx + 90, y + this.imageHeight - 25, 110, 16)) { importHttpClipboard(); return true; }
                    if (clickRect(mouseX, mouseY, cx + 210, y + this.imageHeight - 25, 90, 16)) { formatHttpEditor(); return true; }
                    if (clickRect(mouseX, mouseY, cx + 310, y + this.imageHeight - 25, 70, 16)) { toggleHttpPermission(true); return true; }
                    if (clickRect(mouseX, mouseY, cx + 390, y + this.imageHeight - 25, 70, 16)) { toggleHttpPermission(false); return true; }
                    if (clickRect(mouseX, mouseY, x + this.imageWidth - 150, y + this.imageHeight - 25, 60, 16)) { sendHttp("SAVE"); return true; }
                    if (clickRect(mouseX, mouseY, x + this.imageWidth - 80, y + this.imageHeight - 25, 60, 16)) { sendHttp("DELETE"); return true; }
                } else {
                    if (clickRect(mouseX, mouseY, inputX, y + offset + 0*22 + 6, 15, 10)) { https = !https; httpStatus = "HTTPS changed. Save web settings."; updateVisibility(); return true; }
                    int btnY = y + offset + 2*22 + 10;
                    if (clickRect(mouseX, mouseY, cx, btnY, 80, 16)) { openNewHttpFile(); return true; }
                    if (clickRect(mouseX, mouseY, cx + 90, btnY, 70, 16)) { sendHttp("QUERY"); return true; }
                    if (clickRect(mouseX, mouseY, cx + 170, btnY, 130, 16)) { sendHttp("CONFIG"); return true; }

                    int listTop = y + 240;
                    List<String[]> files = httpFileRows();
                    int visible = Math.min(8, Math.max(0, files.size() - httpFileScroll));
                    for (int i = 0; i < visible; i++) {
                        if (clickRect(mouseX, mouseY, cx, listTop + i * 20, this.imageWidth - 180, 19)) {
                            openHttpFile(files.get(httpFileScroll + i)[0]); return true;
                        }
                    }
                }
            } else if (selectedServiceItem.equals("DNS")) {
                if (clickRect(mouseX, mouseY, inputX, y + offset + 0*22 + 6, 15, 10)) {
                    if (serviceBit(ServerRackService.DNS)) serviceMask &= ~(1L << ServerRackService.DNS.ordinal());
                    else serviceMask |= 1L << ServerRackService.DNS.ordinal();
                    syncLegacyServiceFlags(); updateVisibility(); return true;
                }
                int btnY = y + offset + 4*22 + 10;
                if (clickRect(mouseX, mouseY, cx, btnY, 80, 16)) { dnsRecordType=switch(dnsRecordType){case "A"->"AAAA";case "AAAA"->"CNAME";case "CNAME"->"MX";case "MX"->"PTR";default->"A";}; updateVisibility(); return true; }
                if (clickRect(mouseX, mouseY, cx + 90, btnY, 100, 16)) { sendDns("SAVE"); return true; }
                if (clickRect(mouseX, mouseY, cx + 200, btnY, 80, 16)) { sendDns("REMOVE"); return true; }
                if (clickRect(mouseX, mouseY, cx + 290, btnY, 80, 16)) { sendDns("CLEAR_CACHE"); return true; }
            } else if (selectedServiceItem.equals("DHCP") || selectedServiceItem.equals("DHCPv6")) {
                boolean v6 = selectedServiceItem.equals("DHCPv6");
                int btnY = y + offset + 3*36 + 10;
                if (clickRect(mouseX, mouseY, cx, btnY, 120, 16)) { sendDhcp("SAVE", v6); return true; }
                if (clickRect(mouseX, mouseY, cx + 130, btnY, 90, 16)) { sendDhcp("REMOVE", v6); return true; }
                if (clickRect(mouseX, mouseY, cx + 230, btnY, 90, 16)) { sendDhcp("CLEAR_LEASES", v6); return true; }
            } else if (selectedServiceItem.equals("FTP") || selectedServiceItem.equals("TFTP")) {
                int btnY = selectedServiceItem.equals("FTP") ? y + offset + 22 + 1*36 + 10 : y + offset + 1*22 + 10;
                if (clickRect(mouseX, mouseY, cx, btnY, 80, 16)) { sendTransfer(selectedServiceItem, "CONFIG"); return true; }
                if (selectedServiceItem.equals("FTP")) {
                    if (clickRect(mouseX, mouseY, cx + 90, btnY, 100, 16)) { sendTransfer("FTP", "SAVE_USER"); return true; }
                    if (clickRect(mouseX, mouseY, cx + 200, btnY, 80, 16)) { sendTransfer("FTP", "DELETE_USER"); return true; }
                    if (clickRect(mouseX, mouseY, cx + 290, btnY, 80, 16)) { sendTransfer("FTP", "QUERY"); return true; }
                } else {
                    if (clickRect(mouseX, mouseY, cx + 90, btnY, 80, 16)) { sendTransfer("TFTP", "QUERY"); return true; }
                }
            } else if (selectedServiceItem.equals("EMAIL")) {
                if (clickRect(mouseX, mouseY, inputX, y + offset + 0*22 + 6, 15, 10)) { pop3 = !pop3; updateVisibility(); return true; }
                if (clickRect(mouseX, mouseY, inputX + 210, y + offset + 1*22 + 3, 80, 16)) { serviceStatus = "Mail domain updated."; return true; }
                int btnY = y + offset + 2*22 + 1*36 + 10;
                if (clickRect(mouseX, mouseY, cx, btnY, 100, 16)) { serviceStatus = "Mailbox account added."; return true; }
                if (clickRect(mouseX, mouseY, cx + 110, btnY, 110, 16)) { serviceStatus = "Selected account removed."; return true; }
            } else if (selectedServiceItem.equals("NTP")) {
                if (clickRect(mouseX, mouseY, col0, y + offset + 0*36 + 6, 15, 10)) { ntpServer = !ntpServer; updateVisibility(); return true; }
                if (clickRect(mouseX, mouseY, col1, y + offset + 0*36 + 6, 15, 10)) { ntpClient = !ntpClient; updateVisibility(); return true; }
                int btnY = y + offset + 3*36 + 10;
                if (clickRect(mouseX, mouseY, cx, btnY, 150, 16)) { sendNtp(); return true; }
            } else if (selectedServiceItem.equals("SYSLOG")) {
                if (clickRect(mouseX, mouseY, col1, y + offset + 0*36 + 6, 15, 10)) { syslogAcceptRemote = !syslogAcceptRemote; updateVisibility(); return true; }
                int btnY = y + offset + 3*36 + 10;
                if (clickRect(mouseX, mouseY, cx, btnY, 120, 16)) { sendSyslog("CONFIG"); return true; }
                if (clickRect(mouseX, mouseY, cx + 130, btnY, 80, 16)) { sendSyslog("TEST"); return true; }
                if (clickRect(mouseX, mouseY, cx + 220, btnY, 80, 16)) { sendSyslog("QUERY"); return true; }
                if (clickRect(mouseX, mouseY, cx + 310, btnY, 80, 16)) { sendSyslog("CLEAR"); return true; }
            } else if (selectedServiceItem.equals("AAA")) {
                if (clickRect(mouseX, mouseY, col0, y + offset + 2*36 + 6, 15, 10)) { aaaUserEnabled = !aaaUserEnabled; updateVisibility(); return true; }
                int btnY = y + offset + 3*36 + 10;
                if (clickRect(mouseX, mouseY, cx, btnY, 100, 16)) { sendAaa("SAVE"); return true; }
                if (clickRect(mouseX, mouseY, cx + 110, btnY, 100, 16)) { sendAaa("DELETE"); return true; }
                if (clickRect(mouseX, mouseY, cx + 220, btnY, 80, 16)) { sendAaa("TEST"); return true; }
                if (clickRect(mouseX, mouseY, cx + 310, btnY, 80, 16)) { sendAaa("QUERY"); return true; }
            } else if (selectedServiceItem.equals("RADIUS EAP")) {
                if (clickRect(mouseX, mouseY, col0, y + offset + 2*36 + 6, 15, 10)) { radiusClientEnabled = !radiusClientEnabled; updateVisibility(); return true; }
                int btnY = y + offset + 3*36 + 10;
                if (clickRect(mouseX, mouseY, cx, btnY, 120, 16)) { sendRadius("SAVE"); return true; }
                if (clickRect(mouseX, mouseY, cx + 130, btnY, 100, 16)) { sendRadius("DELETE"); return true; }
                if (clickRect(mouseX, mouseY, cx + 240, btnY, 80, 16)) { sendRadius("TEST"); return true; }
                if (clickRect(mouseX, mouseY, cx + 330, btnY, 80, 16)) { sendRadius("QUERY"); return true; }
            } else if (selectedServiceItem.equals("IoT")) {
                int btnY = y + offset + 2*36 + 10;
                if (clickRect(mouseX, mouseY, cx, btnY, 120, 16)) { sendIot("REGISTER"); return true; }
                if (clickRect(mouseX, mouseY, cx + 130, btnY, 100, 16)) { sendIot("CONTROL"); return true; }
                if (clickRect(mouseX, mouseY, cx + 240, btnY, 120, 16)) { sendIot("TELEMETRY"); return true; }
                if (clickRect(mouseX, mouseY, cx, btnY + 22, 100, 16)) { sendIot("OFFLINE"); return true; }
                if (clickRect(mouseX, mouseY, cx + 110, btnY + 22, 100, 16)) { sendIot("REMOVE"); return true; }
            } else if (selectedServiceItem.equals("VM Management")) {
                int btnY = y + offset + 2*36 + 10;
                if (clickRect(mouseX, mouseY, cx, btnY, 120, 16)) { sendVm("CREATE"); return true; }
                if (clickRect(mouseX, mouseY, cx + 130, btnY, 80, 16)) { sendVm("START"); return true; }
                if (clickRect(mouseX, mouseY, cx + 220, btnY, 80, 16)) { sendVm("STOP"); return true; }
                if (clickRect(mouseX, mouseY, cx + 310, btnY, 80, 16)) { sendVm("RESTART"); return true; }
                if (clickRect(mouseX, mouseY, cx, btnY + 22, 80, 16)) { sendVm("DELETE"); return true; }
            } else if (selectedServiceItem.equals("PRP")) {
                if (clickRect(mouseX, mouseY, inputX, y + offset + 0*22 + 6, 15, 10)) { prpEnabled = !prpEnabled; updateVisibility(); return true; }
                if (clickRect(mouseX, mouseY, inputX, y + offset + 1*22 + 6, 15, 10)) { prpLaneA = !prpLaneA; updateVisibility(); return true; }
                if (clickRect(mouseX, mouseY, inputX, y + offset + 2*22 + 6, 15, 10)) { prpLaneB = !prpLaneB; updateVisibility(); return true; }
                int btnY = y + offset + 4*22 + 10;
                if (clickRect(mouseX, mouseY, cx, btnY, 150, 16)) { sendPrp("CONFIG"); return true; }
                if (clickRect(mouseX, mouseY, cx + 160, btnY, 100, 16)) { sendPrp("QUERY"); return true; }
            }

            ServerRackService service = ServerRackService.byDisplayName(selectedServiceItem);
            List<String[]> rows = interactiveRows(service);
            int visible = interactiveVisibleRows(service);
            int top = interactiveListTop(service);
            if (mouseX >= cx && mouseX < x + this.imageWidth - 16 && mouseY >= top && mouseY < top + visible * 20) {
                int scrollOffset = serviceScrolls.getOrDefault(service.displayName(), 0);
                int index = scrollOffset + (int) ((mouseY - top) / 20);
                if (index >= 0 && index < rows.size()) {
                    selectServiceRow(service, rows.get(index)[1]); return true;
                }
            }

            if (clickRect(mouseX, mouseY, x + this.imageWidth - 160, y + this.imageHeight - 25, 140, 18)) { save(); return true; }
        }

        if (this.currentTab == Tab.DESKTOP) {
            if (desktopPage.isEmpty()) {
                String[] tools = {"IP Configuration", "Terminal", "Command Prompt", "Web Browser", "Email", "DNS Lookup", "Ping", "Text Editor", "FTP Client", "TFTP Client"};
                for (int i = 0; i < tools.length; i++) {
                    int col = i % 4, row = i / 4;
                    if (clickRect(mouseX, mouseY, x + 35 + col * 165, y + 60 + row * 95, 150, 80)) {
                        desktopPage = tools[i]; updateVisibility(); return true;
                    }
                }
            } else {
                if (clickRect(mouseX, mouseY, x + 10, y + 40, 70, 16)) { desktopPage = ""; updateVisibility(); return true; }

                if (desktopPage.equals("Web Browser")) {
                    if (clickRect(mouseX, mouseY, x + 100, y + 40, 50, 16)) { browserBack(); return true; }
                    if (clickRect(mouseX, mouseY, x + 160, y + 40, 50, 16)) { browserForward(); return true; }
                    if (clickRect(mouseX, mouseY, x + 220, y + 40, 50, 16)) { navigateBrowser(browserUrl, false); return true; }
                    if (clickRect(mouseX, mouseY, x + this.imageWidth - 60, y + 58, 40, 16)) { navigateBrowser(toolInput.getValue(), true); return true; }
                    if (!browserFormFields.isEmpty() && clickRect(mouseX, mouseY, x + this.imageWidth - 90, y + this.imageHeight - 120, 70, 16)) { submitBrowserForm(); return true; }

                    int btnY = y + this.imageHeight - 90;
                    for (int i = 0; i < Math.min(browserActions.size(), 8); i++) {
                        if (clickRect(mouseX, mouseY, x + 35 + (i%4)*165, btnY + (i/4)*22, 150, 16)) { runBrowserControl(browserActions.get(i)); return true; }
                    }
                } else if (desktopPage.equals("Email")) {
                    if (!mailClientLoggedIn) {
                        if (clickRect(mouseX, mouseY, x + this.imageWidth/2 - 50, y + 220, 100, 18)) { sendMailClient("LOGIN", ""); return true; }
                    } else if (mailClientCompose) {
                        if (clickRect(mouseX, mouseY, x + 100, y + 250, 80, 18)) { sendMailClient("SEND", ""); return true; }
                        if (clickRect(mouseX, mouseY, x + 190, y + 250, 80, 18)) { mailClientCompose = false; updateVisibility(); return true; }
                    } else if (mailClientReading) {
                        if (clickRect(mouseX, mouseY, x + 20, y + 90, 60, 16)) { mailClientReading = false; updateVisibility(); return true; }
                        if (clickRect(mouseX, mouseY, x + 90, y + 90, 60, 16)) { mailClientCompose = true; mailClientReading = false; mailClientTo = mailClientFrom; mailClientSubject = "Re: " + mailClientSubject; mailClientBody = "\n\n--- Original message ---\n" + mailClientBody; updateVisibility(); return true; }
                        if (clickRect(mouseX, mouseY, x + 160, y + 90, 60, 16)) { sendMailClient("DELETE", mailClientMessageId); return true; }
                    } else {
                        if (clickRect(mouseX, mouseY, x + 20, y + 90, 60, 16)) { openMailFolder("INBOX"); return true; }
                        if (clickRect(mouseX, mouseY, x + 90, y + 90, 60, 16)) { openMailFolder("SENT"); return true; }
                        if (clickRect(mouseX, mouseY, x + 160, y + 90, 80, 16)) { mailClientCompose = true; mailClientReading = false; mailClientTo = ""; mailClientSubject = ""; mailClientBody = ""; updateVisibility(); return true; }
                        if (clickRect(mouseX, mouseY, x + 250, y + 90, 70, 16)) { sendMailClient("LIST", ""); return true; }
                        if (clickRect(mouseX, mouseY, x + this.imageWidth - 90, y + 90, 70, 16)) { mailClientLoggedIn = false; updateVisibility(); return true; }

                        List<String[]> rows = mailClientRows();
                        for (int i = 0; i < Math.min(rows.size(), 12); i++) {
                            if (clickRect(mouseX, mouseY, x + 20, y + 120 + i * 20, this.imageWidth - 40, 18)) { sendMailClient("OPEN", rows.get(i)[0]); return true; }
                        }
                    }
                } else if (desktopPage.equals("Text Editor")) {
                    if (clickRect(mouseX, mouseY, x + 20, y + 60, 60, 16)) { sendTextFile("NEW"); return true; }
                    if (clickRect(mouseX, mouseY, x + 90, y + 60, 70, 16)) { sendTextFile("QUERY"); return true; }
                    if (clickRect(mouseX, mouseY, x + this.imageWidth - 160, y + this.imageHeight - 40, 60, 16)) { sendTextFile("SAVE"); return true; }
                    if (clickRect(mouseX, mouseY, x + this.imageWidth - 90, y + this.imageHeight - 40, 60, 16)) { sendTextFile("DELETE"); return true; }

                    List<String[]> rows = textFileRows();
                    int end = Math.min(rows.size(), textFileScroll + 14);
                    for (int i = textFileScroll; i < end; i++) {
                        if (clickRect(mouseX, mouseY, x + 20, y + 90 + (i - textFileScroll) * 22, 160, 18)) { sendTextFile("OPEN", rows.get(i)[0]); return true; }
                    }
                } else if (desktopPage.equals("FTP Client")) {
                    if (clickRect(mouseX, mouseY, x + 540, y + 58, 80, 16)) { sendFtpClient("CONNECT"); return true; }
                    if (clickRect(mouseX, mouseY, x + 630, y + 58, 70, 16)) { ftpClientConnected = false; ftpClientStatus = "Disconnected."; updateVisibility(); return true; }
                    if (clickRect(mouseX, mouseY, x + 80, y + this.imageHeight - 40, 100, 18)) { sendFtpClient("GET"); return true; }
                    if (clickRect(mouseX, mouseY, x + 400, y + this.imageHeight - 40, 100, 18)) { sendFtpClient("PUT"); return true; }

                    List<String[]> remote = ftpClientRows(ftpClientRemoteFiles);
                    for (int i = ftpClientRemoteScroll; i < Math.min(remote.size(), ftpClientRemoteScroll + 10); i++) {
                        if (clickRect(mouseX, mouseY, x + 20, y + 120 + (i - ftpClientRemoteScroll) * 20, 320, 18)) { ftpClientRemoteField.setValue(remote.get(i)[0]); if(ftpClientLocalField.getValue().isBlank()) ftpClientLocalField.setValue(remote.get(i)[0]); return true; }
                    }
                    List<String[]> local = ftpClientRows(ftpClientLocalFiles);
                    for (int i = ftpClientLocalScroll; i < Math.min(local.size(), ftpClientLocalScroll + 10); i++) {
                        if (clickRect(mouseX, mouseY, x + 360, y + 120 + (i - ftpClientLocalScroll) * 20, 320, 18)) { ftpClientLocalField.setValue(local.get(i)[0]); if(ftpClientRemoteField.getValue().isBlank()) ftpClientRemoteField.setValue(local.get(i)[0]); return true; }
                    }
                } else if (desktopPage.equals("TFTP Client")) {
                    if (clickRect(mouseX, mouseY, x + 220, y + 58, 80, 16)) { sendTftpClient("CONNECT"); return true; }
                    if (clickRect(mouseX, mouseY, x + 310, y + 58, 80, 16)) { tftpClientConnected = false; tftpClientStatus = "Disconnected."; updateVisibility(); return true; }
                    if (clickRect(mouseX, mouseY, x + 80, y + this.imageHeight - 40, 100, 18)) { sendTftpClient("GET"); return true; }
                    if (clickRect(mouseX, mouseY, x + 400, y + this.imageHeight - 40, 100, 18)) { sendTftpClient("PUT"); return true; }
                } else if (desktopPage.equals("IP Configuration")) {
                    if (clickRect(mouseX, mouseY, x + 200, y + 75, 100, 16)) { dhcp = !dhcp; updateVisibility(); return true; }
                    if (clickRect(mouseX, mouseY, x + 200, y + 300, 150, 18)) { saveDesktopNetwork(); return true; }
                } else if (desktopPage.equals("DNS Lookup")) {
                    if (clickRect(mouseX, mouseY, x + 100, y + 60, 100, 16)) { String[] types = {"A","AAAA","CNAME","MX","PTR"}; dnsClientType = types[(java.util.Arrays.asList(types).indexOf(dnsClientType)+1)%types.length]; updateVisibility(); return true; }
                    if (clickRect(mouseX, mouseY, x + 220, y + 60, 80, 16)) { runDesktopDnsLookup(); return true; }
                    if (clickRect(mouseX, mouseY, x + 320, y + 60, 150, 16)) { dnsClientServerField.setValue(dnsClientType.equals("AAAA") ? state.dns6() : state.dns()); return true; }
                } else {
                    if (clickRect(mouseX, mouseY, x + this.imageWidth - 60, y + 58, 40, 16)) { runDesktopTool(); return true; }
                }
            }
        }

        if (this.currentTab == Tab.PROGRAMMING) {
            if (clickRect(mouseX, mouseY, x + 20, y + 40, 60, 16)) { programInput.setValue(""); programOutput = "New program."; return true; }
            if (clickRect(mouseX, mouseY, x + 90, y + 40, 60, 16)) { sendProgram("open"); return true; }
            if (clickRect(mouseX, mouseY, x + 160, y + 40, 60, 16)) { sendProgram("save"); return true; }
            if (clickRect(mouseX, mouseY, x + 230, y + 40, 60, 16)) { sendProgram("run"); return true; }
        }

        for (var child : this.children()) {
            if (child instanceof EditBox e && e.isVisible()) {
                if (e.mouseClicked(mouseX, mouseY, button)) { e.setFocused(true); return true; }
                else e.setFocused(false);
            }
            if (child instanceof MultiLineEditBox m && m.visible) {
                if (m.mouseClicked(mouseX, mouseY, button)) { m.setFocused(true); return true; }
                else m.setFocused(false);
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        if (currentTab == Tab.CONFIG) {
            if (mouseX >= x && mouseX <= x + 160) {
                if (delta > 0 && configScrollOffset > 0) configScrollOffset = Math.max(0.0f, configScrollOffset - 0.1f);
                else if (delta < 0 && configScrollOffset < 1.0f) configScrollOffset = Math.min(1.0f, configScrollOffset + 0.1f);
                return true;
            }
        } else if (currentTab == Tab.SERVICES) {
            if (mouseX >= x && mouseX <= x + 160) {
                if (delta > 0 && servicesScrollOffset > 0) servicesScrollOffset = Math.max(0.0f, servicesScrollOffset - 0.1f);
                else if (delta < 0 && servicesScrollOffset < 1.0f) servicesScrollOffset = Math.min(1.0f, servicesScrollOffset + 0.1f);
                return true;
            } else if (mouseY > y + 190) {
                if (selectedServiceItem.equals("HTTP") && !httpEditorOpen) scrollHttpFiles(delta > 0 ? -1 : 1);
                else if ((selectedServiceItem.equals("FTP") || selectedServiceItem.equals("TFTP")) && !transferEditorOpen) scrollTransferFiles(delta > 0 ? -1 : 1);
                else {
                    ServerRackService service = ServerRackService.byDisplayName(selectedServiceItem);
                    int visible = interactiveVisibleRows(service);
                    scrollService(service, delta > 0 ? -1 : 1, visible);
                }
                return true;
            }
        } else if (currentTab == Tab.DESKTOP) {
            if (desktopPage.equals("Text Editor") && mouseX < x + 185) { scrollTextFiles(delta > 0 ? -1 : 1); return true; }
            if (desktopPage.equals("FTP Client") || desktopPage.equals("TFTP Client")) {
                int amount = delta > 0 ? -1 : 1;
                boolean ftp = desktopPage.equals("FTP Client");
                if (mouseX < x + this.imageWidth / 2) {
                    if (ftp) ftpClientRemoteScroll = Math.max(0, Math.min(Math.max(0, ftpClientRows(ftpClientRemoteFiles).size() - 10), ftpClientRemoteScroll + amount));
                    else tftpClientRemoteScroll = Math.max(0, Math.min(Math.max(0, ftpClientRows(tftpClientRemoteFiles).size() - 11), tftpClientRemoteScroll + amount));
                } else {
                    if (ftp) ftpClientLocalScroll = Math.max(0, Math.min(Math.max(0, ftpClientRows(ftpClientLocalFiles).size() - 10), ftpClientLocalScroll + amount));
                    else tftpClientLocalScroll = Math.max(0, Math.min(Math.max(0, ftpClientRows(tftpClientLocalFiles).size() - 11), tftpClientLocalScroll + amount));
                }
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        boolean handled = false;
        boolean anyFocused = false;

        for (var child : this.children()) {
            if (child instanceof EditBox e && e.isVisible() && e.isFocused()) { anyFocused = true; if (e.keyPressed(pKeyCode, pScanCode, pModifiers)) handled = true; }
            if (child instanceof MultiLineEditBox m && m.visible && m.isFocused()) { anyFocused = true; if (m.keyPressed(pKeyCode, pScanCode, pModifiers)) handled = true; }
        }

        if (handled) return true;
        if (anyFocused && this.minecraft != null && this.minecraft.options.keyInventory.matches(pKeyCode, pScanCode)) return true;

        if (pKeyCode == 256) { this.onClose(); return true; }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        for (var child : this.children()) {
            if (child instanceof EditBox e && e.isVisible() && e.isFocused() && e.charTyped(pCodePoint, pModifiers)) return true;
            if (child instanceof MultiLineEditBox m && m.visible && m.isFocused() && m.charTyped(pCodePoint, pModifiers)) return true;
        }
        return super.charTyped(pCodePoint, pModifiers);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        g.fill(x, y + 30, x + this.imageWidth, y + this.imageHeight, 0xFF1E1E1E);
        g.fill(x, y + 30, x + this.imageWidth, y + 31, 0xFF444444);
        g.fill(x, y + 30, x + 1, y + this.imageHeight, 0xFF444444);
        g.fill(x + this.imageWidth - 1, y + 30, x + this.imageWidth, y + this.imageHeight, 0xFF444444);
        g.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF444444);

        for (int i = 0; i < Tab.values().length; i++) {
            int tabX = x + 10 + (i * 82);
            boolean isActive = (this.currentTab == Tab.values()[i]);

            int bgColor = isActive ? 0xFF1E1E1E : 0xFF2D2D2D;
            int textColor = isActive ? 0xFFFFFFFF : 0xFFAAAAAA;

            g.fill(tabX, y + 10, tabX + 80, y + 31, bgColor);
            g.fill(tabX, y + 10, tabX + 80, y + 11, 0xFF0092C8);
            g.fill(tabX, y + 10, tabX + 1, y + 31, 0xFF444444);
            g.fill(tabX + 79, y + 10, tabX + 80, y + 31, 0xFF444444);

            if (!isActive) g.fill(tabX, y + 30, tabX + 80, y + 31, 0xFF0092C8);

            int textWidth = this.font.width(Tab.values()[i].name());
            g.drawString(this.font, Tab.values()[i].name(), tabX + (40 - textWidth / 2), y + 16, textColor, false);
        }

        switch (this.currentTab) {
            case PHYSICAL -> renderPhysicalTab(g, x, y);
            case CONFIG -> renderConfigTab(g, x, y);
            case SERVICES -> renderServicesTab(g, x, y, mouseX, mouseY);
            case DESKTOP -> renderDesktopTab(g, x, y);
            case PROGRAMMING -> renderProgrammingTab(g, x, y);
            case ATTRIBUTES -> renderAttributesTab(g, x, y);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderPhysicalTab(GuiGraphics g, int x, int y) {
        g.drawString(this.font, "Physical Device View", x + 20, y + 45, 0xFFFFFF, false);

        int px = x + 180;
        int py = y + 60;
        int ph = this.imageHeight - 90;
        int pw = 240;

        // Rack Frame
        g.fill(px, py, px + pw, py + ph, 0xFF111111);
        g.fill(px + 10, py, px + 20, py + ph, 0xFF2A2A2A); // Left rail
        g.fill(px + pw - 20, py, px + pw - 10, py + ph, 0xFF2A2A2A); // Right rail

        // Draw unit markers
        for (int i = 0; i < 15; i++) {
            int sy = py + 10 + (i * 22);
            g.fill(px + 15, sy + 8, px + 17, sy + 10, 0xFF000000);
            g.fill(px + pw - 17, sy + 8, px + pw - 15, sy + 10, 0xFF000000);
        }

        long time = System.currentTimeMillis();

        // Draw inserted servers/modules
        for (int i = 0; i < 12; i++) {
            int sy = py + 10 + (i * 22);
            if (sy + 18 > py + ph) break;

            g.fill(px + 22, sy, px + pw - 22, sy + 18, 0xFF1E1E1E);
            g.fill(px + 22, sy, px + pw - 22, sy + 1, 0xFF444444);
            g.fill(px + 22, sy + 17, px + pw - 22, sy + 18, 0xFF000000);

            // Server details
            g.fill(px + 26, sy + 4, px + 100, sy + 14, 0xFF111111);
            for (int d = 0; d < 4; d++) {
                g.fill(px + 28 + (d * 18), sy + 6, px + 42 + (d * 18), sy + 12, 0xFF333333);
            }

            // LEDs
            int ledColor = ((time + i*133) % 1000) > 400 ? 0xFF22C55E : 0xFF16823B;
            g.fill(px + pw - 34, sy + 8, px + pw - 30, sy + 12, ledColor);

            if (i % 4 == 0) {
                g.fill(px + pw - 42, sy + 8, px + pw - 38, sy + 12, 0xFF0092C8);
            }
        }

        g.pose().pushPose();
        g.pose().translate(px + pw + 30, py + 20, 0);
        g.pose().scale(1.2f, 1.2f, 1.0f);
        g.drawString(this.font, "VSIA SERVER RACK", 0, 0, 0xFF0092C8, false);
        g.pose().popPose();

        g.drawString(this.font, "DATA CENTER EDITION", px + pw + 30, py + 36, 0xFFAAAAAA, false);
        g.drawString(this.font, "Power: ON", px + pw + 30, py + 65, 0xFF22C55E, false);
        g.drawString(this.font, "Platform: 42U High-Density", px + pw + 30, py + 80, 0xFFAAAAAA, false);
        g.drawString(this.font, "Status: Nominal", px + pw + 30, py + 95, 0xFFAAAAAA, false);
        g.drawString(this.font, "Modules Loaded: 12", px + pw + 30, py + 110, 0xFFAAAAAA, false);
    }

    private void renderConfigTab(GuiGraphics g, int x, int y) {
        int sbWidth = 160;
        int listY = y + 31;
        int listHeight = this.imageHeight - 31;

        g.fill(x, listY, x + sbWidth, listY + listHeight, 0xFF1E1E1E);
        g.fill(x + sbWidth, listY, x + sbWidth + 1, listY + listHeight, 0xFF444444);

        int totalItemsHeight = this.configTreeItems.size() * 15;
        this.maxConfigScrollLines = Math.max(0, this.configTreeItems.size() - (listHeight / 15) + 1);
        int startIndex = (int)(this.configScrollOffset * this.maxConfigScrollLines);

        int currentY = listY + 10;
        g.enableScissor(x, listY, x + sbWidth, listY + listHeight);
        for (int i = startIndex; i < this.configTreeItems.size() && currentY < listY + listHeight; i++) {
            String[] item = this.configTreeItems.get(i);
            String text = item[0];
            int color = Long.decode(item[1]).intValue();
            boolean isSelected = item[2].equals("1");
            String type = item[3];

            if (type.equals("empty")) { currentY += 10; continue; }
            if (isSelected) g.fill(x, currentY - 3, x + sbWidth, currentY + 11, 0xFF404040);
            if (type.equals("header")) {
                g.fill(x, currentY - 3, x + sbWidth, currentY + 11, 0xFF1E1E1E);
                g.fill(x, currentY + 10, x + sbWidth, currentY + 11, 0xFF444444);
            }
            g.drawString(this.font, text, x + 10, currentY, color, false);
            currentY += 15;
        }
        g.disableScissor();

        int contentX = x + 180;
        int inputX = contentX + 140;
        int offset = 55;

        g.fill(contentX - 20, y + 31, x + this.imageWidth, y + 45, 0xFF1E1E1E);
        g.fill(contentX - 20, y + 45, x + this.imageWidth, y + 46, 0xFF444444);
        g.drawString(this.font, this.selectedConfigItem, contentX - 20 + ((this.imageWidth - sbWidth) - this.font.width(this.selectedConfigItem))/2, y + 35, 0xFFFFFF, false);

        if (this.selectedConfigItem.equals("Settings")) {
            drawFormBackground(g, contentX, y, 7, offset, 22);
            g.drawString(this.font, "Display Name", contentX, y + offset + 0*22 + 7, 0xFFFFFF, false);

            g.drawString(this.font, "Addressing Mode", contentX, y + offset + 1*22 + 7, 0xFFFFFF, false);
            drawCheckbox(g, inputX, y + offset + 1*22 + 6, dhcp);
            g.drawString(this.font, "DHCP", inputX + 15, y + offset + 1*22 + 7, 0xAAAAAA, false);
            drawCheckbox(g, inputX + 60, y + offset + 1*22 + 6, !dhcp);
            g.drawString(this.font, "Static", inputX + 75, y + offset + 1*22 + 7, 0xAAAAAA, false);

            g.drawString(this.font, "IPv4 Gateway", contentX, y + offset + 2*22 + 7, 0xFFFFFF, false);
            g.drawString(this.font, "IPv4 DNS", contentX, y + offset + 3*22 + 7, 0xFFFFFF, false);

            g.drawString(this.font, "IPv6 Addressing", contentX, y + offset + 4*22 + 7, 0xFFFFFF, false);
            drawCheckbox(g, inputX, y + offset + 4*22 + 6, automatic6);
            g.drawString(this.font, "Auto", inputX + 15, y + offset + 4*22 + 7, 0xAAAAAA, false);
            drawCheckbox(g, inputX + 60, y + offset + 4*22 + 6, !automatic6);
            g.drawString(this.font, "Static", inputX + 75, y + offset + 4*22 + 7, 0xAAAAAA, false);

            g.drawString(this.font, "IPv6 Gateway", contentX, y + offset + 5*22 + 7, 0xFFFFFF, false);
            g.drawString(this.font, "IPv6 DNS", contentX, y + offset + 6*22 + 7, 0xFFFFFF, false);

        } else if (this.selectedConfigItem.equals("FastEthernet0")) {
            drawFormBackground(g, contentX, y, 5, offset, 22);
            g.drawString(this.font, "Port Status", contentX, y + offset + 0*22 + 7, 0xFFFFFF, false);
            drawCheckbox(g, inputX, y + offset + 0*22 + 6, true);
            g.drawString(this.font, "On", inputX + 15, y + offset + 0*22 + 7, 0xAAAAAA, false);

            g.drawString(this.font, "IPv4 Address", contentX, y + offset + 1*22 + 7, 0xFFFFFF, false);
            g.drawString(this.font, "Subnet Mask", contentX, y + offset + 2*22 + 7, 0xFFFFFF, false);

            g.drawString(this.font, "IPv6 Address", contentX, y + offset + 3*22 + 7, 0xFFFFFF, false);
            g.drawString(this.font, "Prefix Length", contentX, y + offset + 4*22 + 7, 0xFFFFFF, false);

        } else if (this.selectedConfigItem.equals("Clock & PTP")) {
            drawFormBackground(g, contentX, y, 3, offset, 22);
            g.drawString(this.font, "Clock Offset (s)", contentX, y + offset + 0*22 + 7, 0xFFFFFF, false);
            g.drawString(this.font, "PTP Mode", contentX, y + offset + 1*22 + 7, 0xFFFFFF, false);
            drawBoxBtn(g, inputX, y + offset + 1*22 + 3, 100, 16, ptpMode);
            g.drawString(this.font, "PTP Profile", contentX, y + offset + 2*22 + 7, 0xFFFFFF, false);
            drawBoxBtn(g, inputX, y + offset + 2*22 + 3, 100, 16, ptpProfile);
        }

        drawBoxBtn(g, x + this.imageWidth - 160, y + this.imageHeight - 25, 140, 18, "Save Configuration");
    }

    private void renderServicesTab(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        int sbWidth = 160;
        int listY = y + 31;
        int listHeight = this.imageHeight - 31;

        g.fill(x, listY, x + sbWidth, listY + listHeight, 0xFF1E1E1E);
        g.fill(x + sbWidth, listY, x + sbWidth + 1, listY + listHeight, 0xFF444444);

        int totalItemsHeight = this.servicesTreeItems.size() * 15;
        this.maxServicesScrollLines = Math.max(0, this.servicesTreeItems.size() - (listHeight / 15) + 1);
        int startIndex = (int)(this.servicesScrollOffset * this.maxServicesScrollLines);

        int currentY = listY + 10;
        g.enableScissor(x, listY, x + sbWidth, listY + listHeight);
        for (int i = startIndex; i < this.servicesTreeItems.size() && currentY < listY + listHeight; i++) {
            String[] item = this.servicesTreeItems.get(i);
            String text = item[0];
            int color = Long.decode(item[1]).intValue();
            boolean isSelected = item[2].equals("1");
            String type = item[3];

            if (type.equals("empty")) { currentY += 10; continue; }
            if (isSelected) g.fill(x, currentY - 3, x + sbWidth, currentY + 11, 0xFF404040);
            if (type.equals("header")) {
                g.fill(x, currentY - 3, x + sbWidth, currentY + 11, 0xFF1E1E1E);
                g.fill(x, currentY + 10, x + sbWidth, currentY + 11, 0xFF444444);
            }
            g.drawString(this.font, text, x + 10, currentY, color, false);
            currentY += 15;
        }
        g.disableScissor();

        int cx = x + 180;
        int inputX = cx + 140;
        int offset = 55;
        int col0 = cx + 5;
        int col1 = cx + 175;
        int col2 = cx + 345;

        g.fill(cx - 20, y + 31, x + this.imageWidth, y + 45, 0xFF1E1E1E);
        g.fill(cx - 20, y + 45, x + this.imageWidth, y + 46, 0xFF444444);
        g.drawString(this.font, this.selectedServiceItem, cx - 20 + ((this.imageWidth - sbWidth) - this.font.width(this.selectedServiceItem))/2, y + 35, 0xFFFFFF, false);

        ServerRackService service = ServerRackService.byDisplayName(selectedServiceItem);

        if (selectedServiceItem.equals("HTTP")) {
            if (httpEditorOpen) {
                drawFormBackground(g, cx, y, 1, offset, 22);
                g.drawString(this.font, "File Name", cx, y + offset + 0*22 + 7, 0xFFFFFF, false);
                drawBoxBtn(g, cx, y + this.imageHeight - 25, 80, 16, "< Back");
                drawBoxBtn(g, cx + 90, y + this.imageHeight - 25, 110, 16, "Import Clipboard");
                drawBoxBtn(g, cx + 210, y + this.imageHeight - 25, 90, 16, "Auto Format");
                drawBoxBtn(g, cx + 310, y + this.imageHeight - 25, 70, 16, httpReadable ? "R [ON]" : "R [OFF]");
                drawBoxBtn(g, cx + 390, y + this.imageHeight - 25, 70, 16, httpWritable ? "W [ON]" : "W [OFF]");
                drawBoxBtn(g, x + this.imageWidth - 150, y + this.imageHeight - 25, 60, 16, "Save");
                drawBoxBtn(g, x + this.imageWidth - 80, y + this.imageHeight - 25, 60, 16, "Delete");
            } else {
                drawFormBackground(g, cx, y, 2, offset, 22);
                g.drawString(this.font, "HTTPS Secure", cx, y + offset + 0*22 + 7, 0xFFFFFF, false);
                drawCheckbox(g, inputX, y + offset + 0*22 + 6, https);
                g.drawString(this.font, "HTTP Port", cx, y + offset + 1*22 + 7, 0xFFFFFF, false);
                g.drawString(this.font, "HTTPS Port", inputX + 70, y + offset + 1*22 + 7, 0xFFFFFF, false);

                int btnY = y + offset + 2*22 + 10;
                drawBoxBtn(g, cx, btnY, 80, 16, "New File");
                drawBoxBtn(g, cx + 90, btnY, 70, 16, "Refresh");
                drawBoxBtn(g, cx + 170, btnY, 130, 16, "Save Web Settings");

                int listTop = y + 240;
                g.fill(cx - 10, listTop, x + this.imageWidth - 10, listTop + 15, 0xFF2A2A2A);
                g.drawString(this.font, "File Name", cx, listTop + 4, 0xFFFFFF, false);
                g.drawString(this.font, "Perms", cx + 200, listTop + 4, 0xFFFFFF, false);
                g.drawString(this.font, "Size", cx + 300, listTop + 4, 0xFFFFFF, false);

                List<String[]> files = httpFileRows();
                int visible = Math.min(8, Math.max(0, files.size() - httpFileScroll));
                for (int i = 0; i < visible; i++) {
                    String[] row = files.get(httpFileScroll + i);
                    int rowY = listTop + 20 + i * 20;
                    g.fill(cx - 10, rowY, x + this.imageWidth - 10, rowY + 19, (i&1)==0 ? 0xFF1E1E1E : 0xFF252525);
                    g.drawString(this.font, font.plainSubstrByWidth(row[0], 190), cx, rowY + 5, 0xEEEEEE, false);
                    g.drawString(this.font, row[1], cx + 200, rowY + 5, 0xDDDDDD, false);
                    g.drawString(this.font, row[2], cx + 300, rowY + 5, 0xDDDDDD, false);
                }
            }
        } else if (selectedServiceItem.equals("DNS")) {
            drawFormBackground(g, cx, y, 4, offset, 22);
            g.drawString(this.font, "DNS Service", cx, y + offset + 0*22 + 7, 0xFFFFFF, false);
            drawCheckbox(g, inputX, y + offset + 0*22 + 6, serviceBit(ServerRackService.DNS));
            g.drawString(this.font, "Name", cx, y + offset + 1*22 + 7, 0xFFFFFF, false);
            g.drawString(this.font, "Detail", cx, y + offset + 2*22 + 7, 0xFFFFFF, false);
            g.drawString(this.font, "TTL", cx, y + offset + 3*22 + 7, 0xFFFFFF, false);

            int btnY = y + offset + 4*22 + 10;
            drawBoxBtn(g, cx, btnY, 80, 16, "Type: " + dnsRecordType);
            drawBoxBtn(g, cx + 90, btnY, 100, 16, "Add / Update");
            drawBoxBtn(g, cx + 200, btnY, 80, 16, "Remove");
            drawBoxBtn(g, cx + 290, btnY, 80, 16, "Clear Cache");
            renderInteractiveListSurface(g, cx, service);
        } else if (selectedServiceItem.equals("DHCP") || selectedServiceItem.equals("DHCPv6")) {
            boolean v6 = selectedServiceItem.equals("DHCPv6");
            drawFormBackground(g, cx, y, 3, offset, 36);
            g.drawString(this.font, "Pool Name", col0, y + offset + 0*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, "Start IP", col1, y + offset + 0*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, "End IP", col2, y + offset + 0*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, v6 ? "Prefix" : "Subnet Mask", col0, y + offset + 1*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, "Gateway", col1, y + offset + 1*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, "DNS", col2, y + offset + 1*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, "Lease (s)", col0, y + offset + 2*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, "Exclusions", col1, y + offset + 2*36 + 4, 0xAAAAAA, false);

            int btnY = y + offset + 3*36 + 10;
            drawBoxBtn(g, cx, btnY, 120, 16, "Add / Update Pool");
            drawBoxBtn(g, cx + 130, btnY, 90, 16, "Remove Pool");
            drawBoxBtn(g, cx + 230, btnY, 90, 16, "Clear Leases");
            renderInteractiveListSurface(g, cx, service);
        } else if (selectedServiceItem.equals("FTP") || selectedServiceItem.equals("TFTP")) {
            if (transferEditorOpen) {
                // Background handled in updateVisibility via MultiLineEditBox replacement logic or direct fields.
            } else {
                int lines = selectedServiceItem.equals("FTP") ? 1 : 0;
                drawFormBackground(g, cx, y, 1, offset, 22);
                g.drawString(this.font, selectedServiceItem + " Port", cx, y + offset + 0*22 + 7, 0xFFFFFF, false);

                int btnY = y + offset + 1*22 + 10;
                if (selectedServiceItem.equals("FTP")) {
                    drawFormBackground(g, cx, y, 1, offset + 22, 36);
                    g.drawString(this.font, "Username", col0, y + offset + 22 + 0*36 + 4, 0xAAAAAA, false);
                    g.drawString(this.font, "Password", col1, y + offset + 22 + 0*36 + 4, 0xAAAAAA, false);
                    btnY = y + offset + 22 + 1*36 + 10;
                }

                drawBoxBtn(g, cx, btnY, 80, 16, "Save Port");
                if (selectedServiceItem.equals("FTP")) {
                    drawBoxBtn(g, cx + 90, btnY, 100, 16, "Add / Update User");
                    drawBoxBtn(g, cx + 200, btnY, 80, 16, "Delete User");
                    drawBoxBtn(g, cx + 290, btnY, 80, 16, "Refresh");
                } else {
                    drawBoxBtn(g, cx + 90, btnY, 80, 16, "Refresh");
                }
            }
        } else if (selectedServiceItem.equals("EMAIL")) {
            drawFormBackground(g, cx, y, 2, offset, 22);
            g.drawString(this.font, "POP3 Service", cx, y + offset + 0*22 + 7, 0xFFFFFF, false);
            drawCheckbox(g, inputX, y + offset + 0*22 + 6, pop3);
            g.drawString(this.font, "Domain", cx, y + offset + 1*22 + 7, 0xFFFFFF, false);
            drawBoxBtn(g, inputX + 210, y + offset + 1*22 + 3, 80, 16, "Set Domain");

            int gridOffset = offset + 2*22;
            drawFormBackground(g, cx, y, 1, gridOffset, 36);
            g.drawString(this.font, "Username", col0, y + gridOffset + 0*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, "Password", col1, y + gridOffset + 0*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, "Quota", col2, y + gridOffset + 0*36 + 4, 0xAAAAAA, false);

            int btnY = y + gridOffset + 1*36 + 10;
            drawBoxBtn(g, cx, btnY, 100, 16, "Add Account");
            drawBoxBtn(g, cx + 110, btnY, 110, 16, "Remove Account");
            renderInteractiveListSurface(g, cx, service);
        } else if (selectedServiceItem.equals("NTP")) {
            drawFormBackground(g, cx, y, 3, offset, 36);
            g.drawString(this.font, "Server Role", col0, y + offset + 0*36 + 12, 0xFFFFFF, false); drawCheckbox(g, col0 + 75, y + offset + 0*36 + 11, ntpServer);
            g.drawString(this.font, "Client Role", col1, y + offset + 0*36 + 12, 0xFFFFFF, false); drawCheckbox(g, col1 + 75, y + offset + 0*36 + 11, ntpClient);
            g.drawString(this.font, "Stratum", col0, y + offset + 1*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, "Poll Interval", col1, y + offset + 1*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, "Source IP", col0, y + offset + 2*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, "Drift", col1, y + offset + 2*36 + 4, 0xAAAAAA, false);

            int btnY = y + offset + 3*36 + 10;
            drawBoxBtn(g, cx, btnY, 150, 16, "Apply Configuration");

            g.drawString(this.font, "Device Time: " + java.time.Instant.ofEpochMilli(ntpDeviceTime).toString(), cx, y + 250, 0xFFFFFF, false);
            g.drawString(this.font, "Status: " + ntpStatus, cx, y + 270, 0x0092C8, false);
        } else if (selectedServiceItem.equals("SYSLOG")) {
            drawFormBackground(g, cx, y, 3, offset, 36);
            g.drawString(this.font, "Min Sev.", col0, y + offset + 0*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, "Accept Remote", col1, y + offset + 0*36 + 12, 0xAAAAAA, false);
            drawCheckbox(g, col1 + 90, y + offset + 0*36 + 11, syslogAcceptRemote);
            g.drawString(this.font, "Facility", col0, y + offset + 1*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, "Severity", col1, y + offset + 1*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, "Message", col0, y + offset + 2*36 + 4, 0xAAAAAA, false);

            int btnY = y + offset + 3*36 + 10;
            drawBoxBtn(g, cx, btnY, 120, 16, "Save Config");
            drawBoxBtn(g, cx + 130, btnY, 80, 16, "Send Test");
            drawBoxBtn(g, cx + 220, btnY, 80, 16, "Refresh");
            drawBoxBtn(g, cx + 310, btnY, 80, 16, "Clear Log");
            renderInteractiveListSurface(g, cx, service);
        } else if (selectedServiceItem.equals("AAA")) {
            drawFormBackground(g, cx, y, 3, offset, 36);
            g.drawString(this.font, "Username", col0, y + offset + 0*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, "Password", col1, y + offset + 0*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, "Privilege", col0, y + offset + 1*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, "Service", col1, y + offset + 1*36 + 4, 0xAAAAAA, false);
            drawCheckbox(g, col0, y + offset + 2*36 + 11, aaaUserEnabled); g.drawString(this.font, "Account Enabled", col0 + 15, y + offset + 2*36 + 12, 0xFFFFFF, false);

            int btnY = y + offset + 3*36 + 10;
            drawBoxBtn(g, cx, btnY, 100, 16, "Add / Update");
            drawBoxBtn(g, cx + 110, btnY, 100, 16, "Remove");
            drawBoxBtn(g, cx + 220, btnY, 80, 16, "Test Login");
            drawBoxBtn(g, cx + 310, btnY, 80, 16, "Refresh");
            renderInteractiveListSurface(g, cx, service);
        } else if (selectedServiceItem.equals("RADIUS EAP")) {
            drawFormBackground(g, cx, y, 3, offset, 36);
            g.drawString(this.font, "Client Name", col0, y + offset + 0*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, "IPv4 Address", col1, y + offset + 0*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, "Shared Secret", col2, y + offset + 0*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, "Test User", col0, y + offset + 1*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, "Test Pass", col1, y + offset + 1*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, "Test Priv", col2, y + offset + 1*36 + 4, 0xAAAAAA, false);
            drawCheckbox(g, col0, y + offset + 2*36 + 11, radiusClientEnabled); g.drawString(this.font, "Client Enabled", col0 + 15, y + offset + 2*36 + 12, 0xFFFFFF, false);

            int btnY = y + offset + 3*36 + 10;
            drawBoxBtn(g, cx, btnY, 120, 16, "Add / Update");
            drawBoxBtn(g, cx + 130, btnY, 100, 16, "Remove");
            drawBoxBtn(g, cx + 240, btnY, 80, 16, "Test EAP");
            drawBoxBtn(g, cx + 330, btnY, 80, 16, "Refresh");
            renderInteractiveListSurface(g, cx, service);
        } else if (selectedServiceItem.equals("IoT")) {
            drawFormBackground(g, cx, y, 2, offset, 36);
            g.drawString(this.font, "Device ID", col0, y + offset + 0*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, "Display Name", col1, y + offset + 0*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, "Type", col2, y + offset + 0*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, "State / Telemetry", col0, y + offset + 1*36 + 4, 0xAAAAAA, false);

            int btnY = y + offset + 2*36 + 10;
            drawBoxBtn(g, cx, btnY, 120, 16, "Register / Update");
            drawBoxBtn(g, cx + 130, btnY, 100, 16, "Apply Control");
            drawBoxBtn(g, cx + 240, btnY, 120, 16, "Update Telemetry");
            drawBoxBtn(g, cx, btnY + 22, 100, 16, "Mark Offline");
            drawBoxBtn(g, cx + 110, btnY + 22, 100, 16, "Remove");
            renderInteractiveListSurface(g, cx, service);
        } else if (selectedServiceItem.equals("VM Management")) {
            drawFormBackground(g, cx, y, 2, offset, 36);
            g.drawString(this.font, "VM Name", col0, y + offset + 0*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, "Operating System", col1, y + offset + 0*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, "CPU Cores", col0, y + offset + 1*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, "Memory (MB)", col1, y + offset + 1*36 + 4, 0xAAAAAA, false);
            g.drawString(this.font, "Storage (GB)", col2, y + offset + 1*36 + 4, 0xAAAAAA, false);

            int btnY = y + offset + 2*36 + 10;
            drawBoxBtn(g, cx, btnY, 120, 16, "Create / Update");
            drawBoxBtn(g, cx + 130, btnY, 80, 16, "Start");
            drawBoxBtn(g, cx + 220, btnY, 80, 16, "Stop");
            drawBoxBtn(g, cx + 310, btnY, 80, 16, "Restart");
            drawBoxBtn(g, cx, btnY + 22, 80, 16, "Delete");
            renderInteractiveListSurface(g, cx, service);
        } else if (selectedServiceItem.equals("PRP")) {
            drawFormBackground(g, cx, y, 4, offset, 22);
            g.drawString(this.font, "PRP Enabled", cx, y + offset + 0*22 + 7, 0xFFFFFF, false); drawCheckbox(g, inputX, y + offset + 0*22 + 6, prpEnabled);
            g.drawString(this.font, "LAN A Up", cx, y + offset + 1*22 + 7, 0xFFFFFF, false); drawCheckbox(g, inputX, y + offset + 1*22 + 6, prpLaneA);
            g.drawString(this.font, "LAN B Up", cx, y + offset + 2*22 + 7, 0xFFFFFF, false); drawCheckbox(g, inputX, y + offset + 2*22 + 6, prpLaneB);
            g.drawString(this.font, "Peer IPv4", cx, y + offset + 3*22 + 7, 0xFFFFFF, false);

            int btnY = y + offset + 4*22 + 10;
            drawBoxBtn(g, cx, btnY, 150, 16, "Apply Configuration");
            drawBoxBtn(g, cx + 160, btnY, 100, 16, "Refresh");
        } else {
            g.drawString(this.font, "Service module ready for next update.", cx, y + 65, 0x888888, false);
        }

        drawBoxBtn(g, x + this.imageWidth - 160, y + this.imageHeight - 25, 140, 18, "Save Services");
    }

    private void renderInteractiveListSurface(GuiGraphics g, int cx, ServerRackService service) {
        List<String[]> rows = interactiveRows(service);
        if (rows.isEmpty()) return;

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int visible = interactiveVisibleRows(service);
        int top = interactiveListTop(service);
        int right = x + this.imageWidth - 28;
        int width = right - cx;
        int firstX = cx + 8, secondX = cx + width/3, thirdX = cx + (width*2)/3;

        g.fill(cx - 10, top - 23, right, top + visible * 20 + 1, 0xFF1E1E1E);
        g.fill(cx - 10, top - 22, right, top - 2, 0xFF2A2A2A);

        String[] headers = interactiveHeaders(service);
        drawClipped(g, headers[0], firstX, top - 16, width/3 - 12, 0xFFFFFF);
        drawClipped(g, headers[1], secondX, top - 16, width/3 - 12, 0xFFFFFF);
        drawClipped(g, headers[2], thirdX, top - 16, width/3 - 12, 0xFFFFFF);

        int offset = serviceScrolls.getOrDefault(service.displayName(), 0);
        int shown = Math.min(visible, rows.size() - offset);

        for (int i = 0; i < shown; i++) {
            int ry = top + i * 20;
            g.fill(cx - 10, ry, right, ry + 19, (i&1)==0 ? 0xFF1E1E1E : 0xFF252525);
            String[] columns = interactiveColumns(service, rows.get(offset + i)[1]);
            drawClipped(g, columns[0], firstX, ry + 5, width/3 - 12, 0xEEEEEE);
            drawClipped(g, columns[1], secondX, ry + 5, width/3 - 12, 0xDDDDDD);
            drawClipped(g, columns[2], thirdX, ry + 5, width/3 - 12, 0xDDDDDD);
        }
    }

    private int interactiveListTop(ServerRackService service) {
        int y = (this.height - this.imageHeight) / 2;
        return y + 240;
    }

    private int interactiveVisibleRows(ServerRackService service) {
        return 8;
    }

    private void renderDesktopTab(GuiGraphics g, int x, int y) {
        if (desktopPage.isEmpty()) {
            String[] tools = {"IP Configuration", "Terminal", "Command Prompt", "Web Browser", "Email", "DNS Lookup", "Ping", "Text Editor", "FTP Client", "TFTP Client"};
            for (int i = 0; i < tools.length; i++) {
                int col = i % 4, row = i / 4;
                int bx = x + 35 + col * 165;
                int by = y + 60 + row * 95;
                g.fill(bx, by, bx + 150, by + 80, 0xFF2A2A2A);
                g.fill(bx + 1, by + 1, bx + 149, by + 79, 0xFF1E1E1E);
                g.drawCenteredString(this.font, tools[i], bx + 75, by + 35, 0xFFFFFF);
            }
            return;
        }

        g.fill(x + 5, y + 35, x + this.imageWidth - 5, y + 58, 0xFF2A2A2A);
        drawBoxBtn(g, x + 10, y + 40, 70, 16, "< Home");
        g.drawCenteredString(this.font, desktopPage, x + this.imageWidth / 2, y + 44, 0xFFFFFF);

        if (desktopPage.equals("Web Browser")) {
            drawBoxBtn(g, x + 100, y + 40, 50, 16, "Back");
            drawBoxBtn(g, x + 160, y + 40, 50, 16, "Fwd");
            drawBoxBtn(g, x + 220, y + 40, 50, 16, "Ref");

            drawFormBackground(g, x + 100, y, 1, 55, 22);
            drawBoxBtn(g, x + this.imageWidth - 60, y + 58, 40, 16, "Go");

            int left = x + 20, right = x + this.imageWidth - 20, top = y + 80, bottom = y + this.imageHeight - 130;
            g.fill(left, top, right, bottom, browserBackground);
            g.hLine(left, right, top, 0xFF888888);
            g.drawCenteredString(font, browserTitle, x + this.imageWidth / 2, top + 12, browserForeground);
            g.drawString(font, "HTTP " + (browserStatus == 0 ? "Ready" : browserStatus), left + 10, top + 12, browserStatus >= 400 ? 0xFFAA2222 : 0xFF228844);

            int contentBottom = browserNotice.isBlank() ? bottom - 14 : bottom - 38;
            int lineY = top + 34;
            outer:
            for (String raw : browserPageText.split("\\n", -1)) {
                for (FormattedCharSequence line : font.split(Component.literal(raw), right - left - 24)) {
                    if (lineY > contentBottom) break outer;
                    g.drawString(font, line, left + 12, lineY, browserForeground);
                    lineY += 12;
                }
                if (raw.isBlank()) lineY += 5;
            }

            int btnY = y + this.imageHeight - 90;
            for (int i = 0; i < Math.min(browserActions.size(), 8); i++) {
                drawBoxBtn(g, x + 35 + (i%4)*165, btnY + (i/4)*22, 150, 16, browserActions.get(i)[1]);
            }
            if (!browserFormFields.isEmpty()) {
                drawFormBackground(g, x + 35, y + this.imageHeight - 150, 1, 0, 45);
                drawBoxBtn(g, x + this.imageWidth - 90, y + this.imageHeight - 120, 70, 16, "Submit");
            }

        } else if (desktopPage.equals("Email")) {
            if (!mailClientLoggedIn) {
                drawFormBackground(g, x + 250, y, 2, 145, 22);
                g.drawString(this.font, "Mailbox", x + 180, y + 154, 0xAAAAAA, false);
                g.drawString(this.font, "Password", x + 180, y + 184, 0xAAAAAA, false);
                drawBoxBtn(g, x + this.imageWidth/2 - 50, y + 220, 100, 18, "Sign In");
                g.drawCenteredString(this.font, mailClientStatus, x + this.imageWidth/2, y + 250, 0x88CCFF);
            } else if (mailClientCompose) {
                drawFormBackground(g, x + 100, y, 3, 95, 22);
                g.drawString(this.font, "To", x + 40, y + 104, 0xAAAAAA, false);
                g.drawString(this.font, "Subject", x + 40, y + 134, 0xAAAAAA, false);
                g.drawString(this.font, "Message", x + 40, y + 164, 0xAAAAAA, false);
                drawBoxBtn(g, x + 100, y + 250, 80, 18, "Send");
                drawBoxBtn(g, x + 190, y + 250, 80, 18, "Cancel");
            } else if (mailClientReading) {
                g.drawString(this.font, mailClientSubject, x + 20, y + 120, 0xFFFFFF, false);
                g.drawString(this.font, "From: " + mailClientFrom, x + 20, y + 140, 0xAAAAAA, false);
                g.drawString(this.font, "To: " + mailClientTo, x + 20, y + 155, 0xAAAAAA, false);
                g.fill(x + 20, y + 180, x + this.imageWidth - 20, y + this.imageHeight - 20, 0xFF181818);

                int lineY = y + 190;
                for (FormattedCharSequence line : font.split(Component.literal(mailClientBody), this.imageWidth - 40)) {
                    g.drawString(font, line, x + 30, lineY, 0xEEEEEE, false);
                    lineY += 12;
                    if (lineY > y + this.imageHeight - 30) break;
                }

                drawBoxBtn(g, x + 20, y + 90, 60, 16, "Back");
                drawBoxBtn(g, x + 90, y + 90, 60, 16, "Reply");
                drawBoxBtn(g, x + 160, y + 90, 60, 16, "Delete");
            } else {
                drawBoxBtn(g, x + 20, y + 90, 60, 16, "Inbox");
                drawBoxBtn(g, x + 90, y + 90, 60, 16, "Sent");
                drawBoxBtn(g, x + 160, y + 90, 80, 16, "Compose");
                drawBoxBtn(g, x + 250, y + 90, 70, 16, "Refresh");
                drawBoxBtn(g, x + this.imageWidth - 90, y + 90, 70, 16, "Sign Out");

                int top = y + 120;
                g.fill(x + 20, top, x + this.imageWidth - 20, top + 18, 0xFF2A2A2A);
                g.drawString(this.font, mailClientFolder, x + 30, top + 5, 0xFFFFFF, false);

                List<String[]> rows = mailClientRows();
                for (int i = 0; i < Math.min(rows.size(), 12); i++) {
                    String[] row = rows.get(i);
                    g.fill(x + 20, top + 20 + i*20, x + this.imageWidth - 20, top + 39 + i*20, (i&1)==0 ? 0xFF1E1E1E : 0xFF252525);
                    String marker = Boolean.parseBoolean(row[1]) ? "  " : "* ";
                    String corr = mailClientFolder.equals("SENT") ? row[3] : row[2];
                    g.drawString(this.font, marker + font.plainSubstrByWidth(row[4], 200) + "  -  " + corr, x + 30, top + 25 + i*20, 0xEEEEEE, false);
                }
            }

        } else if (desktopPage.equals("Text Editor")) {
            drawBoxBtn(g, x + 20, y + 60, 60, 16, "New");
            drawBoxBtn(g, x + 90, y + 60, 70, 16, "Refresh");
            drawBoxBtn(g, x + this.imageWidth - 160, y + this.imageHeight - 40, 60, 16, "Save");
            drawBoxBtn(g, x + this.imageWidth - 90, y + this.imageHeight - 40, 60, 16, "Delete");
            drawFormBackground(g, x + 200, y, 1, 55, 22);

            List<String[]> rows = textFileRows();
            int end = Math.min(rows.size(), textFileScroll + 14);
            for (int i = textFileScroll; i < end; i++) {
                String[] row = rows.get(i);
                int ry = y + 90 + (i - textFileScroll) * 22;
                g.fill(x + 20, ry, x + 180, ry + 18, 0xFF2A2A2A);
                g.drawString(this.font, row[0], x + 25, ry + 5, 0xEEEEEE, false);
            }
            g.drawString(this.font, textFileStatus, x + 200, y + this.imageHeight - 35, 0x88CCFF, false);

        } else if (desktopPage.equals("FTP Client")) {
            drawFormBackground(g, x + 80, y, 1, 55, 22);
            drawFormBackground(g, x + 260, y, 1, 55, 22);
            drawFormBackground(g, x + 430, y, 1, 55, 22);
            g.drawString(this.font, "IP", x + 20, y + 64, 0xAAAAAA, false);
            g.drawString(this.font, "User", x + 220, y + 64, 0xAAAAAA, false);
            g.drawString(this.font, "Pass", x + 380, y + 64, 0xAAAAAA, false);

            drawBoxBtn(g, x + 540, y + 58, 80, 16, "Connect");
            drawBoxBtn(g, x + 630, y + 58, 70, 16, "Disconn");

            drawFormBackground(g, x + 80, y, 1, this.imageHeight - 105, 22);
            drawFormBackground(g, x + 400, y, 1, this.imageHeight - 105, 22);
            drawBoxBtn(g, x + 80, y + this.imageHeight - 40, 100, 18, "Download ->");
            drawBoxBtn(g, x + 400, y + this.imageHeight - 40, 100, 18, "<- Upload");

            g.drawString(this.font, "Remote", x + 20, y + 100, 0xFFFFFF, false);
            g.drawString(this.font, "Local", x + 350, y + 100, 0xFFFFFF, false);

            List<String[]> remote = ftpClientRows(ftpClientRemoteFiles);
            for (int i = ftpClientRemoteScroll; i < Math.min(remote.size(), ftpClientRemoteScroll + 10); i++) {
                g.fill(x + 20, y + 120 + (i - ftpClientRemoteScroll) * 20, x + 340, y + 138 + (i - ftpClientRemoteScroll) * 20, 0xFF2A2A2A);
                g.drawString(this.font, remote.get(i)[0], x + 25, y + 125 + (i - ftpClientRemoteScroll) * 20, 0xEEEEEE, false);
            }
            List<String[]> local = ftpClientRows(ftpClientLocalFiles);
            for (int i = ftpClientLocalScroll; i < Math.min(local.size(), ftpClientLocalScroll + 10); i++) {
                g.fill(x + 360, y + 120 + (i - ftpClientLocalScroll) * 20, x + 680, y + 138 + (i - ftpClientLocalScroll) * 20, 0xFF2A2A2A);
                g.drawString(this.font, local.get(i)[0], x + 365, y + 125 + (i - ftpClientLocalScroll) * 20, 0xEEEEEE, false);
            }

        } else if (desktopPage.equals("TFTP Client")) {
            drawFormBackground(g, x + 80, y, 1, 55, 22);
            g.drawString(this.font, "Server IP", x + 20, y + 64, 0xAAAAAA, false);
            drawBoxBtn(g, x + 220, y + 58, 80, 16, "Connect");
            drawBoxBtn(g, x + 310, y + 58, 80, 16, "Disconn");
            drawFormBackground(g, x + 80, y, 1, this.imageHeight - 105, 22);
            drawFormBackground(g, x + 400, y, 1, this.imageHeight - 105, 22);
            drawBoxBtn(g, x + 80, y + this.imageHeight - 40, 100, 18, "Read ->");
            drawBoxBtn(g, x + 400, y + this.imageHeight - 40, 100, 18, "<- Write");
            g.drawString(this.font, "Remote", x + 20, y + 100, 0xFFFFFF, false);
            g.drawString(this.font, "Local", x + 350, y + 100, 0xFFFFFF, false);
        } else if (desktopPage.equals("IP Configuration")) {
            drawFormBackground(g, x + 200, y, 4, 95, 22);
            drawFormBackground(g, x + 200, y, 4, 205, 22);
            g.drawString(this.font, "IPv4 Config", x + 50, y + 104, 0xFFFFFF, false);
            g.drawString(this.font, "IPv6 Config", x + 50, y + 214, 0xFFFFFF, false);

            g.drawString(this.font, "DHCP / Static", x + 50, y + 78, 0xAAAAAA, false);
            drawCheckbox(g, x + 200, y + 78, dhcp);

            drawBoxBtn(g, x + 200, y + 300, 150, 18, "Apply Desktop Network");
        } else if (desktopPage.equals("DNS Lookup")) {
            drawFormBackground(g, x + 100, y, 2, 85, 22);
            g.drawString(this.font, "Domain", x + 20, y + 94, 0xAAAAAA, false);
            g.drawString(this.font, "DNS Server", x + 20, y + 124, 0xAAAAAA, false);
            drawBoxBtn(g, x + 100, y + 60, 100, 16, "Type: " + dnsClientType);
            drawBoxBtn(g, x + 220, y + 60, 80, 16, "Lookup");
            drawBoxBtn(g, x + 320, y + 60, 150, 16, "Use Configured DNS");

            g.fill(x + 20, y + 160, x + this.imageWidth - 20, y + this.imageHeight - 20, 0xFF080808);
            int lineY = y + 170;
            for (String raw : desktopOutput.split("\\n", -1)) {
                for (FormattedCharSequence line : font.split(Component.literal(raw), this.imageWidth - 60)) {
                    g.drawString(font, line, x + 30, lineY, 0xB8FFB8, false);
                    lineY += 12;
                    if (lineY > y + this.imageHeight - 30) break;
                }
            }
        } else {
            drawFormBackground(g, x + 100, y, 1, 55, 22);
            g.drawString(this.font, "Input", x + 20, y + 64, 0xAAAAAA, false);
            drawBoxBtn(g, x + this.imageWidth - 60, y + 58, 40, 16, "Run");

            g.fill(x + 20, y + 90, x + this.imageWidth - 20, y + this.imageHeight - 20, 0xFF080808);
            int lineY = y + 100;
            for (String raw : desktopOutput.split("\\n", -1)) {
                for (FormattedCharSequence line : font.split(Component.literal(raw), this.imageWidth - 60)) {
                    g.drawString(font, line, x + 30, lineY, 0xB8FFB8, false);
                    lineY += 12;
                    if (lineY > y + this.imageHeight - 30) break;
                }
            }
        }
    }

    private void renderProgrammingTab(GuiGraphics g, int x, int y) {
        drawBoxBtn(g, x + 20, y + 40, 60, 16, "New");
        drawBoxBtn(g, x + 90, y + 40, 60, 16, "Open");
        drawBoxBtn(g, x + 160, y + 40, 60, 16, "Save");
        drawBoxBtn(g, x + 230, y + 40, 60, 16, "Run");

        drawFormBackground(g, x + 20, y, 1, 75, 22);

        g.drawString(this.font, "Console Output", x + 20, y + 115, 0xAAAAAA, false);
        g.fill(x + 20, y + 130, x + this.imageWidth - 20, y + this.imageHeight - 20, 0xFF080808);

        int lineY = y + 140;
        for (String raw : programOutput.split("\\n", -1)) {
            for (FormattedCharSequence line : font.split(Component.literal(raw), this.imageWidth - 60)) {
                g.drawString(font, line, x + 30, lineY, 0xB8FFB8, false);
                lineY += 12;
                if (lineY > y + this.imageHeight - 30) break;
            }
        }
    }

    private void renderAttributesTab(GuiGraphics g, int x, int y) {
        int tableX = x + 20;
        int tableY = y + 50;

        g.drawString(this.font, "Attribute", tableX, tableY, 0xFFFFFF, false);
        g.drawString(this.font, "Value", tableX + 200, tableY, 0xFFFFFF, false);
        g.fill(tableX, tableY + 12, x + this.imageWidth - 20, tableY + 13, 0xFF444444);

        int rowY = tableY + 20;
        String[][] attributes = {
                {"Mean Time Between Failures", "61,320 hours"}, {"Cost", "$ 2,000"}, {"Power Source", "Internal 120V AC"},
                {"Rack Units", "42U High-Density"}, {"Power Consumption", "200 W (Base)"}, {"Device Model", "VSIA Server Rack"},
                {"IPv4 Address", state.ip()}, {"IPv6 Address", state.ipv6()+"/"+state.ipv6Prefix()},
                {"PTP Settings", state.ptpMode() + " / " + state.ptpProfile()}, {"World Position", state.pos().toShortString()}
        };
        for (String[] attr : attributes) {
            g.drawString(this.font, attr[0], tableX, rowY, 0xAAAAAA, false);
            g.drawString(this.font, attr[1], tableX + 200, rowY, 0xAAAAAA, false);
            rowY += 20;
        }
    }

    private void drawFormBackground(GuiGraphics g, int contentX, int y, int rows, int startYOffset, int rowHeight) {
        int guiX = (this.width - this.imageWidth) / 2;
        int startY = y + startYOffset;
        int endY = startY + (rowHeight * rows) - 1;
        int rightEdge = guiX + this.imageWidth - 10;

        g.fill(contentX - 10, startY, rightEdge, endY, 0xFF1E1E1E);
        for (int i = 0; i <= rows; i++) {
            g.fill(contentX - 10, startY + (i * rowHeight), rightEdge, startY + (i * rowHeight) + 1, 0xFF444444);
        }
        g.fill(contentX - 10, startY, contentX - 9, endY, 0xFF444444);
        g.fill(rightEdge - 1, startY, rightEdge, endY, 0xFF444444);
    }

    private void drawBoxBtn(GuiGraphics g, int x, int y, int w, int h, String text) {
        g.fill(x, y, x + w, y + h, 0xFF2A2A2A);
        g.fill(x, y, x + w, y + 1, 0xFF444444);
        g.fill(x, y + h - 1, x + w, y + h, 0xFF444444);
        g.fill(x, y, x + 1, y + h, 0xFF444444);
        g.fill(x + w - 1, y, x + w, y + h, 0xFF444444);
        int tw = this.font.width(text);
        g.drawString(this.font, text, x + (w - tw)/2, y + 4, 0xFFFFFF, false);
    }

    private void drawCheckbox(GuiGraphics g, int x, int y, boolean checked) {
        g.fill(x, y, x + 9, y + 9, 0xFFFFFFFF);
        g.fill(x, y, x + 9, y + 1, 0xFF888888);
        g.fill(x, y, x + 1, y + 9, 0xFF888888);
        if (checked) g.fill(x + 2, y + 2, x + 7, y + 7, 0xFF0078D7);
    }

    private void drawClipped(GuiGraphics g, String value, int drawX, int drawY, int maxWidth, int color) {
        g.drawString(font, font.plainSubstrByWidth(value, Math.max(1, maxWidth)), drawX, drawY, color, false);
    }

    private String[] interactiveHeaders(ServerRackService service) { return switch(service) { case DNS->new String[]{"Name","Record Type","Address / TTL"}; case DHCP,DHCPV6->new String[]{"Pool","Address Range","Network Settings"}; case EMAIL->new String[]{"Mailbox","Quota","Messages"}; case SYSLOG->new String[]{"Facility","Severity","Message"}; case AAA->new String[]{"Username","Privilege","Status"}; case RADIUS_EAP->new String[]{"NAS Client","Address","Status"}; case IOT->new String[]{"Device","Type","State / Telemetry"}; case VM_MANAGEMENT->new String[]{"Virtual Machine","OS / Resources","State"}; default->new String[]{"Name","Details","Status"}; }; }
    private String[] interactiveColumns(ServerRackService service,String raw){String[] p=raw.split("\\t",-1);return switch(service){case DNS->new String[]{valueAt(p,0),valueAt(p,1),valueAt(p,2)+(p.length>3?"  TTL "+p[3]:"")};case DHCP,DHCPV6->new String[]{valueAt(p,0),valueAt(p,1)+" - "+valueAt(p,2),valueAt(p,3)+" / "+valueAt(p,4)};case EMAIL->new String[]{valueAt(p,0),valueAt(p,1),"Open mailbox"};case SYSLOG->new String[]{valueAt(p,2),severityName(parseIntText(valueAt(p,3))),valueAt(p,4)};case AAA->new String[]{valueAt(p,0),"Level "+valueAt(p,1),Boolean.parseBoolean(valueAt(p,2))?"Enabled":"Disabled"};case RADIUS_EAP->new String[]{valueAt(p,0),valueAt(p,1),Boolean.parseBoolean(valueAt(p,2))?"Enabled":"Disabled"};case IOT->new String[]{valueAt(p,1),valueAt(p,2),valueAt(p,4).isBlank()?valueAt(p,5):valueAt(p,4)};case VM_MANAGEMENT->new String[]{valueAt(p,0),valueAt(p,1)+" / "+valueAt(p,2)+" CPU",valueAt(p,5)};default->new String[]{valueAt(p,0),valueAt(p,1),valueAt(p,2)};};}
    private String valueAt(String[] values,int index){return index<values.length?values[index]:"";}
    private void scrollService(ServerRackService service,int delta,int visible){List<String[]> rows=interactiveRows(service);String key=service.displayName();int value=Math.max(0,Math.min(Math.max(0,rows.size()-visible),serviceScrolls.getOrDefault(key,0)+delta));serviceScrolls.put(key,value);updateVisibility();}
    private void selectServiceRow(ServerRackService service,String raw){String[] p=raw.split("\\t",-1);switch(service){case DNS->{if(p.length>=4){dnsName.setValue(p[0]);dnsRecordType=p[1];dnsDetail.setValue(p[2]);dnsTtl.setValue(p[3]);dnsStatus="Record loaded.";}}case DHCP,DHCPV6->{if(p.length>=8){poolName.setValue(p[0]);poolStart.setValue(p[1]);poolEnd.setValue(p[2]);poolPrefix.setValue(p[3]);poolGateway.setValue(p[4]);poolDns.setValue(p[5]);poolLease.setValue(p[6]);poolExclusions.setValue(p[7]);dhcpStatus="Pool loaded.";}}case EMAIL->{String[] address=p[0].split("@",2);mailUserField.setValue(address[0]);if(address.length>1)mailDomainField.setValue(address[1]);mailQuotaField.setValue(p.length>1?p[1]:"100");mailPasswordField.setValue("");serviceStatus="Mailbox loaded. Enter its password before saving changes.";}case SYSLOG->{if(p.length>=5){syslogFacilityField.setValue(p[2]);syslogSeverityField.setValue(p[3]);syslogMessageField.setValue(p[4]);syslogStatus="Log details loaded into the test fields.";}}case AAA->{if(p.length>=3){aaaUserField.setValue(p[0]);aaaPasswordField.setValue("");aaaPrivilegeField.setValue(p[1]);aaaUserEnabled=Boolean.parseBoolean(p[2]);aaaStatus="User loaded. Enter a password before updating.";}}case RADIUS_EAP->{if(p.length>=3){radiusNameField.setValue(p[0]);radiusAddressField.setValue(p[1]);radiusSecretField.setValue("");radiusClientEnabled=Boolean.parseBoolean(p[2]);radiusStatus="NAS client loaded. Enter its shared secret before updating.";}}case IOT->{if(p.length>=6){iotIdField.setValue(p[0]);iotNameField.setValue(p[1]);iotTypeField.setValue(p[2]);iotValueField.setValue(p[4].isBlank()?p[5]:p[4]);iotStatus="IoT device loaded for editing.";}}case VM_MANAGEMENT->{if(p.length>=5){vmNameField.setValue(p[0]);vmOsField.setValue(p[1]);vmCpuField.setValue(p[2]);vmMemoryField.setValue(p[3]);vmStorageField.setValue(p[4]);vmStatus="Virtual machine loaded for editing.";}}default->{}} updateVisibility(); }

    private List<String[]> interactiveRows(ServerRackService service){List<String[]> rows=new ArrayList<>();String data=switch(service){case DNS->dnsRecordData;case DHCP->dhcp4Data;case DHCPV6->dhcp6Data;case SYSLOG->syslogData;case AAA->aaaUsers;case RADIUS_EAP->radiusClients;case IOT->iotDevices;case VM_MANAGEMENT->virtualMachines;default->"";};if(service==ServerRackService.EMAIL){rows.add(new String[]{"admin@vsia-net.com    Quota 100","admin@vsia-net.com\t100"});rows.add(new String[]{"player@vsia-net.com    Quota 100","player@vsia-net.com\t100"});return rows;}if(data==null||data.isBlank())return rows;boolean pools=service==ServerRackService.DHCP||service==ServerRackService.DHCPV6;boolean inPools=!pools;for(String line:data.strip().split("\\n")){if(pools&&line.equals("POOLS")){inPools=true;continue;}if(pools&&line.equals("LEASES")){inPools=false;continue;}if(!inPools||line.isBlank())continue;String[] p=line.split("\\t",-1);String label=switch(service){case DNS->p[0]+"  ["+(p.length>1?p[1]:"")+"]    "+(p.length>2?p[2]:"");case DHCP,DHCPV6->"Pool "+p[0]+"    "+(p.length>2?p[1]+" - "+p[2]:"");case SYSLOG->(p.length>4?p[2]+" / "+severityName(parseIntText(p[3]))+"    "+p[4]:line);case AAA->p[0]+"    Privilege "+(p.length>1?p[1]:"")+"    "+(p.length>2&&Boolean.parseBoolean(p[2])?"Enabled":"Disabled");case RADIUS_EAP->p[0]+"    "+(p.length>1?p[1]:"")+"    "+(p.length>2&&Boolean.parseBoolean(p[2])?"Enabled":"Disabled");case IOT->(p.length>2?p[1]+" ["+p[2]+"]    "+p[4]:line);case VM_MANAGEMENT->(p.length>5?p[0]+" ["+p[1]+"]    "+p[5]:line);default->line;};rows.add(new String[]{label,line});}return rows;}

    private void syncLegacyServiceFlags(){http=serviceBit(ServerRackService.HTTP);dhcpService=serviceBit(ServerRackService.DHCP);dnsService=serviceBit(ServerRackService.DNS);mail=serviceBit(ServerRackService.EMAIL);}
    private boolean serviceBit(ServerRackService service){return(serviceMask&(1L<<service.ordinal()))!=0;}

    private void save(){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.SaveConfigPacket(state.pos(), name==null?state.displayName():name.getValue(),ip==null?state.ip():ip.getValue(),subnet==null?state.subnet():subnet.getValue(), gateway==null?state.gateway():gateway.getValue(),dns==null?state.dns():dns.getValue(),dhcp,http,dnsService,dhcpService,mail, ipv6==null?state.ipv6():ipv6.getValue(),parseInt(prefix6,state.ipv6Prefix()),gateway6==null?state.gateway6():gateway6.getValue(),dns6==null?state.dns6():dns6.getValue(),automatic6,parseLong(clockOffset,state.clockOffset()/1000L)*1000L,ptpMode,ptpProfile,serviceMask));}
    private int parseInt(EditBox box,int fallback){try{return box==null?fallback:Integer.parseInt(box.getValue());}catch(Exception e){return fallback;}}
    private long parseLong(EditBox box,long fallback){try{return box==null?fallback:Long.parseLong(box.getValue());}catch(Exception e){return fallback;}}
    private void runDesktopTool(){desktopOutput="Running...";ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.DesktopToolPacket(state.pos(),desktopPage,toolInput==null?"":toolInput.getValue()));}
    public static void acceptDesktopResult(String tool,String result){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen&&screen.desktopPage.equals(tool)){if(tool.equals("Web Browser"))screen.acceptBrowserPage(result);else screen.desktopOutput=result;}}
    private void saveDesktopNetwork(){desktopOutput="Saving network configuration...";ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.DesktopNetworkConfigPacket(state.pos(),dhcp,ip.getValue(),subnet.getValue(),gateway.getValue(),dns.getValue(),ipv6.getValue(),parseInt(prefix6,state.ipv6Prefix()),gateway6.getValue(),dns6.getValue()));}
    private void runDesktopDnsLookup(){String query=dnsClientNameField.getValue().trim()+" "+dnsClientType+" "+dnsClientServerField.getValue().trim();desktopOutput="Querying DNS server...";ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.DesktopToolPacket(state.pos(),desktopPage,query.trim()));}
    private List<String[]> textFileRows(){List<String[]> rows=new ArrayList<>();if(textFileData==null||textFileData.isBlank())return rows;for(String line:textFileData.strip().split("\\n")){String[] p=line.split("\\t",-1);if(p.length>=2)rows.add(p);}return rows;}
    private void scrollTextFiles(int amount){textFileScroll=Math.max(0,Math.min(Math.max(0,textFileRows().size()-14),textFileScroll+amount));updateVisibility();}
    private void sendTextFile(String action){sendTextFile(action,textFileNameField==null?textFileName:textFileNameField.getValue());}
    private void sendTextFile(String action,String filename){String content=textFileEditor==null?textFileContent:textFileEditor.getValue();textFileStatus="Working...";ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.TextFileCommandPacket(state.pos(),action,filename,content));}
    public static void acceptTextFileResult(String message,String files,String filename,String content){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen&&screen.desktopPage.equals("Text Editor")){screen.textFileStatus=message;screen.textFileData=files;if(!filename.isEmpty()||message.equals("Document deleted.")){screen.textFileName=filename;screen.textFileContent=content;}screen.textFileScroll=Math.min(screen.textFileScroll,Math.max(0,screen.textFileRows().size()-14));screen.updateVisibility();}}
    private List<String[]> ftpClientRows(String data){List<String[]> rows=new ArrayList<>();if(data==null||data.isBlank())return rows;for(String line:data.strip().split("\\n")){String[] p=line.split("\\t",-1);if(p.length>=2)rows.add(p);}return rows;}
    private void sendFtpClient(String action){if(ftpClientServerField!=null)ftpClientServer=ftpClientServerField.getValue();if(ftpClientUserField!=null)ftpClientUser=ftpClientUserField.getValue();if(ftpClientPasswordField!=null)ftpClientPassword=ftpClientPasswordField.getValue();ftpClientStatus="Working...";ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.FtpClientCommandPacket(state.pos(),action,ftpClientServer,ftpClientUser,ftpClientPassword,ftpClientRemoteField==null?"":ftpClientRemoteField.getValue(),ftpClientLocalField==null?"":ftpClientLocalField.getValue()));}
    public static void acceptFtpClientResult(String message,boolean connected,String server,String username,String remoteFiles,String localFiles){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen&&screen.desktopPage.equals("FTP Client")){screen.ftpClientStatus=message;screen.ftpClientConnected=connected;screen.ftpClientServer=server.isBlank()?screen.ftpClientServer:server;screen.ftpClientUser=username.isBlank()?screen.ftpClientUser:username;screen.ftpClientRemoteFiles=remoteFiles;screen.ftpClientLocalFiles=localFiles;screen.updateVisibility();}}
    private void sendTftpClient(String action){if(tftpClientServerField!=null)tftpClientServer=tftpClientServerField.getValue();tftpClientStatus="Working...";ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.TftpClientCommandPacket(state.pos(),action,tftpClientServer,tftpClientRemoteField==null?"":tftpClientRemoteField.getValue(),tftpClientLocalField==null?"":tftpClientLocalField.getValue()));}
    public static void acceptTftpClientResult(String message,boolean connected,String server,String remoteFiles,String localFiles){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen&&screen.desktopPage.equals("TFTP Client")){screen.tftpClientStatus=message;screen.tftpClientConnected=connected;screen.tftpClientServer=server.isBlank()?screen.tftpClientServer:server;screen.tftpClientRemoteFiles=remoteFiles;screen.tftpClientLocalFiles=localFiles;screen.updateVisibility();}}
    private List<String[]> mailClientRows(){List<String[]> rows=new ArrayList<>();if(mailClientData==null||mailClientData.isBlank())return rows;for(String line:mailClientData.strip().split("\\n")){String[] values=line.split("\\t",-1);if(values.length>=6)rows.add(values);}return rows;}
    private void openMailFolder(String folder){mailClientFolder=folder;mailClientCompose=false;mailClientReading=false;sendMailClient("LIST","");}
    private void sendMailClient(String action,String id){if(mailClientAddressField!=null)mailClientAddress=mailClientAddressField.getValue().trim().toLowerCase();if(mailClientPasswordField!=null&&!mailClientPasswordField.getValue().isEmpty())mailClientPassword=mailClientPasswordField.getValue();String to=mailClientToField==null?mailClientTo:mailClientToField.getValue();String subject=mailClientSubjectField==null?mailClientSubject:mailClientSubjectField.getValue();String body=mailClientBodyField==null?mailClientBody:mailClientBodyField.getValue();mailClientStatus="Working...";ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.MailClientCommandPacket(state.pos(),action,mailClientAddress,mailClientPassword,mailClientFolder,id,to,subject,body));}
    public static void acceptMailClientResult(String message,boolean authenticated,String address,String folder,String data,String id,String from,String to,String subject,String body,long sentAt){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.mailClientStatus=message;if(authenticated){screen.mailClientLoggedIn=true;screen.mailClientAddress=address;screen.mailClientFolder=folder;screen.mailClientData=data;}if(!id.isEmpty()){screen.mailClientReading=true;screen.mailClientCompose=false;screen.mailClientMessageId=id;screen.mailClientFrom=from;screen.mailClientTo=to;screen.mailClientSubject=subject;screen.mailClientBody=body;screen.mailClientSentAt=sentAt;}else if(message.startsWith("Message delivered")||message.startsWith("Message deleted")){screen.mailClientReading=false;screen.mailClientCompose=false;}screen.updateVisibility();}}
    private void navigateBrowser(String url,boolean addHistory){url=url.trim();if(url.isEmpty())return;if(!url.matches("(?i)^https?://.*"))url="http://"+url;if(addHistory){while(browserHistory.size()>browserHistoryIndex+1)browserHistory.remove(browserHistory.size()-1);browserHistory.add(url);browserHistoryIndex=browserHistory.size()-1;}browserUrl=url;browserTitle="Loading...";browserPageText="Contacting server...";browserLinks.clear();browserActions.clear();browserFormFields.clear();browserFormWidgets.clear();browserFormAction="";browserNotice="";ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.DesktopToolPacket(state.pos(),"Web Browser",url));}
    private void browserBack(){if(browserHistoryIndex>0){browserHistoryIndex--;navigateBrowser(browserHistory.get(browserHistoryIndex),false);}}
    private void browserForward(){if(browserHistoryIndex+1<browserHistory.size()){browserHistoryIndex++;navigateBrowser(browserHistory.get(browserHistoryIndex),false);}}
    private String resolveBrowserLink(String href){if(href.matches("(?i)^https?://.*"))return href;String base=browserUrl;int scheme=base.indexOf("://")+3;int slash=base.indexOf('/',scheme);String root=slash<0?base:base.substring(0,slash);if(href.startsWith("/"))return root+href;int last=base.lastIndexOf('/');return (last>=scheme?base.substring(0,last+1):base+"/")+href;}
    private void acceptBrowserPage(String result){browserLinks.clear();browserActions.clear();browserFormFields.clear();browserFormWidgets.clear();browserFormAction="";browserNotice="";browserBackground=0xFFF4F4F4;browserForeground=0xFF202020;if(!result.startsWith("VSIA_BROWSER\t")){browserStatus=500;browserTitle="Invalid Response";browserPageText=result;updateVisibility();return;}int newline=result.indexOf('\n');String header=newline<0?result:result.substring(0,newline);String html=newline<0?"":result.substring(newline+1);String[] parts=header.split("\t",4);try{browserStatus=Integer.parseInt(parts[1]);}catch(Exception e){browserStatus=500;}if(parts.length>2&&!parts[2].isBlank())browserUrl=parts[2];browserTitle=parts.length>3?parts[3]:"VSIA Browser";Matcher titleMatcher=Pattern.compile("(?is)<title[^>]*>(.*?)</title>").matcher(html);if(titleMatcher.find())browserTitle=cleanHtml(titleMatcher.group(1));Matcher colors=Pattern.compile("(?is)(?:body\\s*\\{|<body[^>]*style=['\"])(.*?)(?:\\}|['\"])").matcher(html);if(colors.find()){Matcher bg=Pattern.compile("(?i)background(?:-color)?\\s*:\\s*(#[0-9a-f]{6}|black|white|navy)").matcher(colors.group(1));Matcher fg=Pattern.compile("(?i)(?:^|;)\\s*color\\s*:\\s*(#[0-9a-f]{6}|black|white|navy)").matcher(colors.group(1));if(bg.find())browserBackground=cssColor(bg.group(1),browserBackground);if(fg.find())browserForeground=cssColor(fg.group(1),browserForeground);}Matcher links=Pattern.compile("(?is)<a\\s+[^>]*href\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>(.*?)</a>").matcher(html);while(links.find()&&browserLinks.size()<8){String label=cleanHtml(links.group(2));browserLinks.add(new String[]{label.isBlank()?links.group(1):label,links.group(1)});}Map<String,String> functions=new HashMap<>();Matcher functionMatcher=Pattern.compile("(?is)function\\s+([A-Za-z_$][\\w$]*)\\s*\\([^)]*\\)\\s*\\{(.*?)\\}").matcher(html);while(functionMatcher.find())functions.put(functionMatcher.group(1),functionMatcher.group(2));Matcher buttons=Pattern.compile("(?is)<button\\b([^>]*)>(.*?)</button>").matcher(html);while(buttons.find()&&browserLinks.size()+browserActions.size()<8){Matcher click=Pattern.compile("(?is)onclick\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')").matcher(buttons.group(1));if(!click.find())continue;String code=click.group(1)!=null?click.group(1):click.group(2);Matcher call=Pattern.compile("^\\s*([A-Za-z_$][\\w$]*)\\s*\\(\\s*\\)\\s*;?\\s*$").matcher(code);if(call.matches()&&functions.containsKey(call.group(1)))code=functions.get(call.group(1));String label=cleanHtml(buttons.group(2));browserActions.add(new String[]{"SCRIPT",label.isBlank()?"Run action":label,decodeEntities(code)});}Matcher form=Pattern.compile("(?is)<form\\b([^>]*)>(.*?)</form>").matcher(html);if(form.find()){Matcher action=Pattern.compile("(?is)action\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')").matcher(form.group(1));browserFormAction=action.find()?(action.group(1)!=null?action.group(1):action.group(2)):browserUrl;Matcher inputs=Pattern.compile("(?is)<input\\b([^>]*)>").matcher(form.group(2));while(inputs.find()){String attrs=inputs.group(1);String type=htmlAttribute(attrs,"type","text");if(!type.equalsIgnoreCase("text")&&!type.equalsIgnoreCase("password")&&!type.equalsIgnoreCase("search"))continue;String fieldName=htmlAttribute(attrs,"name","");if(fieldName.isBlank())continue;browserFormFields.add(new String[]{fieldName,htmlAttribute(attrs,"value",""),htmlAttribute(attrs,"placeholder",fieldName)});}for (int i = 0; i < Math.min(browserFormFields.size(), 3); i++) {EditBox input = new EditBox(font, 0, 0, 100, 12, Component.empty()); input.setHint(Component.literal(browserFormFields.get(i)[2].isBlank() ? browserFormFields.get(i)[0] : browserFormFields.get(i)[2])); addRenderableWidget(input); browserFormWidgets.add(input);}}String text=html.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>","").replaceAll("(?i)<br\\s*/?>|</p>|</h[1-6]>|</li>|</div>","\n").replaceAll("(?s)<[^>]+>","");browserPageText=decodeEntities(text).replaceAll("[ \\t]+"," ").replaceAll("\\n\\s*\\n+","\n\n").trim();if(browserPageText.isBlank())browserPageText="This page has no visible text.";updateVisibility();}
    private void runBrowserControl(String[] control){if(control.length<3)return;if("NAV".equals(control[0]))navigateBrowser(resolveBrowserLink(control[2]),true);else runBrowserScript(control[2]);}
    private void submitBrowserForm(){if(browserFormFields.isEmpty())return;StringBuilder query=new StringBuilder();for(int i=0;i<browserFormFields.size()&&i<browserFormWidgets.size();i++){if(query.length()>0)query.append('&');query.append(urlEncode(browserFormFields.get(i)[0])).append('=').append(urlEncode(browserFormWidgets.get(i).getValue()));}String target=browserFormAction.isBlank()?browserUrl:resolveBrowserLink(browserFormAction);target+=(target.contains("?")?"&":"?")+query;navigateBrowser(target,true);}
    private static String htmlAttribute(String attributes,String name,String fallback){Matcher matcher=Pattern.compile("(?is)\\b"+Pattern.quote(name)+"\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))").matcher(attributes);if(!matcher.find())return fallback;String value=matcher.group(1)!=null?matcher.group(1):matcher.group(2)!=null?matcher.group(2):matcher.group(3);return decodeEntities(value);}
    private static String urlEncode(String value){return java.net.URLEncoder.encode(value,java.nio.charset.StandardCharsets.UTF_8).replace("+","%20");}
    private void runBrowserScript(String script){if(script==null||script.length()>1024){browserNotice="Blocked: the website action is too large.";updateVisibility();return;}Matcher navigation=Pattern.compile("(?is)(?:window\\.)?location(?:\\.href)?\\s*=\\s*['\"]([^'\"]+)['\"]").matcher(script);if(navigation.find()){navigateBrowser(resolveBrowserLink(navigation.group(1)),true);return;}Matcher message=Pattern.compile("(?is)(?:alert|console\\.log)\\s*\\(\\s*['\"]([^'\"]*)['\"]\\s*\\)").matcher(script);if(message.find()){browserNotice=decodeEntities(message.group(1));updateVisibility();return;}Matcher textUpdate=Pattern.compile("(?is)document\\.getElementById\\s*\\(\\s*['\"][^'\"]+['\"]\\s*\\)\\s*\\.(?:innerText|textContent)\\s*=\\s*['\"]([^'\"]*)['\"]").matcher(script);if(textUpdate.find()){browserNotice=decodeEntities(textUpdate.group(1));updateVisibility();return;}browserNotice="Blocked unsupported website action. Allowed: navigation, alert, console.log, and text updates.";updateVisibility();}
    private static int cssColor(String value,int fallback){return switch(value.toLowerCase()){case "black"->0xFF000000;case "white"->0xFFFFFFFF;case "navy"->0xFF000080;default->{try{yield 0xFF000000|Integer.parseInt(value.substring(1),16);}catch(Exception e){yield fallback;}}};}
    private static String cleanHtml(String value){return decodeEntities(value.replaceAll("(?s)<[^>]+>","")).trim();}
    private static String decodeEntities(String value){return value.replace("&lt;","<").replace("&gt;",">").replace("&amp;","&").replace("&quot;","\"").replace("&#39;", "'").replace("&nbsp;"," ");}
    private void sendDns(String action){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.DnsRecordPacket(state.pos(),action,dnsName==null?"":dnsName.getValue(),dnsRecordType,dnsDetail==null?"":dnsDetail.getValue(),parseInt(dnsTtl,300)));}
    public static void acceptDnsResult(String message,String records){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.dnsStatus=message;screen.dnsRecordData=records;screen.updateVisibility();}}
    private void sendDhcp(String action,boolean v6){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.DhcpPoolPacket(state.pos(),action,poolName.getValue(),v6,poolStart.getValue(),poolEnd.getValue(),poolPrefix.getValue(),poolGateway.getValue(),poolDns.getValue(),parseInt(poolLease,3600),poolExclusions.getValue()));}
    public static void acceptDhcpResult(String message,String data,boolean ipv6){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.dhcpStatus=message;if(ipv6)screen.dhcp6Data=data;else screen.dhcp4Data=data;screen.updateVisibility();}}
    private void sendNtp(){serviceStatus="Saving NTP configuration...";ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.NtpConfigPacket(state.pos(),ntpServer,ntpClient,parseInt(ntpStratumField,state.ntpStratum()),parseInt(ntpPollField,state.ntpPoll()),ntpSourceField.getValue(),parseInt(ntpDriftField,state.clockDrift())));}
    public static void acceptNtpResult(String message,String status,long deviceTime,long lastSync){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.serviceStatus=message;screen.ntpStatus=status;screen.ntpDeviceTime=deviceTime;screen.lastNtpSync=lastSync;screen.updateVisibility();}}
    private void sendSyslog(String action){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.SyslogCommandPacket(state.pos(),action,parseInt(syslogMinField,7),syslogAcceptRemote,syslogFacilityField==null?"LOCAL0":syslogFacilityField.getValue(),parseInt(syslogSeverityField,6),syslogMessageField==null?"":syslogMessageField.getValue()));}
    public static void acceptSyslogResult(String message,String data,int minimumSeverity,boolean acceptRemote){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.syslogStatus=message;screen.syslogData=data;screen.syslogAcceptRemote=acceptRemote;if(screen.syslogMinField!=null)screen.syslogMinField.setValue(Integer.toString(minimumSeverity));screen.updateVisibility();}}
    private void sendAaa(String action){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.AaaCommandPacket(state.pos(),action,aaaUserField==null?"":aaaUserField.getValue(),aaaPasswordField==null?"":aaaPasswordField.getValue(),parseInt(aaaPrivilegeField,1),aaaUserEnabled,aaaServiceField==null?"LOGIN":aaaServiceField.getValue()));}
    public static void acceptAaaResult(String message,String users,String accounting){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.aaaStatus=message;screen.aaaUsers=users;screen.aaaAccounting=accounting;screen.updateVisibility();}}
    private void sendRadius(String action){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.RadiusCommandPacket(state.pos(),action,radiusNameField==null?"":radiusNameField.getValue(),radiusAddressField==null?"":radiusAddressField.getValue(),radiusSecretField==null?"":radiusSecretField.getValue(),radiusClientEnabled,radiusUserField==null?"":radiusUserField.getValue(),radiusPasswordField==null?"":radiusPasswordField.getValue(),parseInt(radiusPrivilegeField,1)));}
    public static void acceptRadiusResult(String message,String clients,String events){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.radiusStatus=message;screen.radiusClients=clients;screen.radiusEvents=events;screen.updateVisibility();}}
    private void sendIot(String action){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.IotCommandPacket(state.pos(),action,iotIdField==null?"":iotIdField.getValue(),iotNameField==null?"":iotNameField.getValue(),iotTypeField==null?"":iotTypeField.getValue(),iotValueField==null?"":iotValueField.getValue()));}
    public static void acceptIotResult(String message,String devices){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.iotStatus=message;screen.iotDevices=devices;screen.updateVisibility();}}
    private void sendVm(String action){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.VmCommandPacket(state.pos(),action,vmNameField==null?"":vmNameField.getValue(),vmOsField==null?"":vmOsField.getValue(),parseInt(vmCpuField,2),parseInt(vmMemoryField,4096),parseInt(vmStorageField,64)));}
    public static void acceptVmResult(String message,String machines){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.vmStatus=message;screen.virtualMachines=machines;screen.updateVisibility();}}
    private void sendPrp(String action){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.PrpCommandPacket(state.pos(),action,prpEnabled,prpLaneA,prpLaneB,prpPeerField==null?"":prpPeerField.getValue()));}
    public static void acceptPrpResult(String message,String status){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.prpStatus=message;screen.prpData=status;String[] p=status.split("\\t",-1);if(p.length>=4){screen.prpEnabled=p[0].equals("ENABLED");screen.prpLaneA=p[1].equals("UP");screen.prpLaneB=p[2].equals("UP");if(screen.prpPeerField!=null)screen.prpPeerField.setValue(p[3]);}screen.updateVisibility();}}
    private void sendHttp(String action){String content=httpMultiLineEditor!=null?httpMultiLineEditor.getValue():httpContentField==null?"":httpContentField.getValue();ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.HttpFileCommandPacket(state.pos(),action,httpFileNameField==null?"":httpFileNameField.getValue(),content,httpReadable,httpWritable,https,parseInt(httpPortField,80),parseInt(httpsPortField,443)));}
    private void toggleHttpPermission(boolean readable){httpContentField=hiddenHttpContent(httpMultiLineEditor.getValue());if(readable)httpReadable=!httpReadable;else httpWritable=!httpWritable;updateVisibility();}
    private List<String[]> httpFileRows(){List<String[]> rows=new ArrayList<>();if(httpFiles==null||httpFiles.isBlank())return rows;for(String line:httpFiles.strip().split("\\n")){String[] p=line.split("\\t",-1);if(p.length>=4)rows.add(new String[]{p[0],(p[1].equals("true")?"R":"-")+(p[2].equals("true")?"W":"-"),p[3]+" B"});}return rows;}
    private void scrollHttpFiles(int delta){int max=Math.max(0,httpFileRows().size()-12);httpFileScroll=Math.max(0,Math.min(max,httpFileScroll+delta));updateVisibility();}
    private void openHttpFile(String filename){httpFileNameField=hiddenHttpContent(filename);httpStatus="Loading "+filename+"...";sendHttp("OPEN");}
    private void openNewHttpFile(){httpEditorOpen=true;httpFileNameField=hiddenHttpContent("new-page.html");httpContentField=hiddenHttpContent("<!DOCTYPE html>\n<html>\n<head>\n<title>New Page</title>\n</head>\n<body>\n<h1>New Page</h1>\n</body>\n</html>");httpReadable=true;httpWritable=true;httpFileNameField.setValue("new-page.html");httpMultiLineEditor.setValue(httpContentField.getValue());updateVisibility();}
    private void importHttpClipboard(){String value=net.minecraft.client.Minecraft.getInstance().keyboardHandler.getClipboard();if(value.length()>32768)value=value.substring(0,32768);if(httpMultiLineEditor!=null)httpMultiLineEditor.setValue(value);else if(httpContentField!=null)httpContentField.setValue(value);httpStatus="Clipboard imported. Press Save to store it on the rack.";}
    private void formatHttpEditor(){if(httpMultiLineEditor==null)return;String name=httpFileNameField.getValue().toLowerCase();String source=httpMultiLineEditor.getValue();String formatted=name.endsWith(".html")||name.endsWith(".htm")?formatMarkup(source):formatBraces(source);httpMultiLineEditor.setValue(formatted);httpStatus="Formatting complete. Review the result, then press Save.";}
    private static String formatMarkup(String source){String prepared=source.replaceAll(">\\s*<",">\n<");StringBuilder out=new StringBuilder();int depth=0;for(String raw:prepared.split("\\n")){String line=raw.trim();if(line.isEmpty())continue;boolean closing=line.matches("(?i)^</.*");boolean singleton=line.matches("(?i).*?/?>$")&&(line.matches("(?i)^<(br|hr|img|input|link|meta|!doctype).*"));if(closing)depth=Math.max(0,depth-1);out.append("  ".repeat(depth)).append(line).append('\n');if(line.matches("(?i)^<[^/!][^>]*>.*")&&!line.matches("(?is).*?</[^>]+>.*")&&!singleton)depth++;}return out.toString().stripTrailing();}
    private static String formatBraces(String source){String prepared=source.replace("{","{\n").replace("}","\n}\n").replace(";",";\n");StringBuilder out=new StringBuilder();int depth=0;for(String raw:prepared.split("\\n")){String line=raw.trim();if(line.isEmpty())continue;if(line.startsWith("}"))depth=Math.max(0,depth-1);out.append("  ".repeat(depth)).append(line).append('\n');if(line.endsWith("{"))depth++;}return out.toString().stripTrailing();}
    public static void acceptHttpFileResult(String message,String files,String filename,String content,boolean readable,boolean writable,boolean secure,int httpPort,int httpsPort){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.httpStatus=message;screen.httpFiles=files;screen.httpReadable=readable;screen.httpWritable=writable;screen.https=secure;screen.configuredHttpPort=httpPort;screen.configuredHttpsPort=httpsPort;if(!filename.isEmpty()){screen.httpEditorOpen=true;screen.httpFileNameField=screen.hiddenHttpContent(filename);screen.httpContentField=screen.hiddenHttpContent(content);}if(!filename.isEmpty()){screen.httpFileNameField.setValue(filename);screen.httpMultiLineEditor.setValue(content);}screen.updateVisibility();}}
    private void sendTransfer(String protocol,String action){ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.TransferFileCommandPacket(state.pos(),protocol,action,transferFileNameField==null?"":transferFileNameField.getValue(),transferContentField==null?"":transferContentField.getValue(),transferReadable,transferWritable,transferUserField==null?"":transferUserField.getValue(),transferPasswordField==null?"":transferPasswordField.getValue(),parseInt(transferPortField,protocol.equals("FTP")?21:69)));}
    private List<String[]> transferFileRows(){List<String[]> rows=new ArrayList<>();if(transferFiles==null||transferFiles.isBlank())return rows;for(String line:transferFiles.strip().split("\\n")){String[] p=line.split("\\t",-1);if(p.length>=4)rows.add(new String[]{p[0],(p[1].equals("true")?"R":"-")+(p[2].equals("true")?"W":"-"),p[3]+" B"});}return rows;}
    private void openTransferFile(String protocol,String filename){transferFileNameField=hiddenHttpContent(filename);transferStatus="Loading "+filename+"...";sendTransfer(protocol,"OPEN");}
    private void openNewTransferFile(){transferEditorOpen=true;transferFileNameField=hiddenHttpContent("new-file.txt");transferContentField=hiddenHttpContent("");transferReadable=true;transferWritable=true;updateVisibility();}
    private void scrollTransferFiles(int delta){transferFileScroll=Math.max(0,Math.min(Math.max(0,transferFileRows().size()-10),transferFileScroll+delta));updateVisibility();}
    public static void acceptTransferFileResult(String protocol,String message,String files,String users,String filename,String content,boolean readable,boolean writable,int port){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){screen.transferStatus=message;screen.transferFiles=files;screen.transferUsers=users;screen.transferReadable=readable;screen.transferWritable=writable;if(protocol.equals("FTP"))screen.configuredFtpPort=port;else screen.configuredTftpPort=port;if(!filename.isEmpty()){screen.transferEditorOpen=true;screen.transferFileNameField=screen.hiddenHttpContent(filename);screen.transferContentField=screen.hiddenHttpContent(content);}screen.updateVisibility();}}
    private void sendProgram(String action){programOutput=action.equals("run")?"Running...":"Working...";ServerRackNetwork.CHANNEL.sendToServer(new ServerRackNetwork.ProgramPacket(state.pos(),action,programInput==null?"":programInput.getValue()));}
    public static void acceptProgramResult(String source,String result){if(net.minecraft.client.Minecraft.getInstance().screen instanceof ServerRackScreen screen){if(screen.programInput!=null)screen.programInput.setValue(source);screen.programOutput=result;}}
    private long parseLongText(String value){try{return Long.parseLong(value);}catch(Exception e){return 0;}}
    private int parseIntText(String value){try{return Integer.parseInt(value);}catch(Exception e){return 7;}}
    private String severityName(int value){return switch(value){case 0->"Emergency";case 1->"Alert";case 2->"Critical";case 3->"Error";case 4->"Warning";case 5->"Notice";case 6->"Informational";default->"Debug";};}

    @Override
    public boolean isPauseScreen() { return false; }
}