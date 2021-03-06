<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>

<head>
<title>User space</title>
<#include "/ftl/layout/common_includes.ftl">
<script language="javascript">
function confirmReset() { return confirm("All filtered news will be removed. Are you sure?"); }
function hideHelp()     { 
  $('#dl_div').attr('style', 'background:none;style:none');
  $('#dl_help').hide();
  $('#dl_show').show();
  $('#dl_hide').hide();
}
function showHelp()     { 
  $('#dl_div').attr('style', 'background:#f8f8f8;style:1px solid #aaa');
  $('#dl_help').show();
  $('#dl_show').hide();
  $('#dl_hide').show();
}
</script>
<style>
tr.issue_row    { vertical-align: top; text-align: right; }
tr.issue_row td { padding-bottom:5px; border-bottom: 1px dotted #aaa;}
tr.issue_row a  { font-weight: bold; text-decoration: none; }
div.edit_bar    { font-size: 10px; margin-top: 5px; }
div.edit_bar a  { text-decoration:none; font-weight: normal; color: #800; }
dl#dl_help      { display: none; }
dt              { text-align: left; font-weight: bold; }
dd              { text-align: left; margin-bottom: 10px; }
span#dl_show,
span#dl_hide    { font-weight:bold; font-size:10px; color:#a00; float:right; }
span#dl_hide    { display:none; }
</style>
</head>

<body>

<@s.set name="user" value="#session.user" />

<@s.if test="#user">
<div class="bodymain">
<table class="userhome">
	<#include "/ftl/layout/header.ftl">
	<tr>
	<#include "/ftl/layout/left.menu.ftl">
	<td class="user_space">
	<#include "/ftl/layout/errors.ftl">
	<#include "/ftl/layout/messages.ftl">
  <h1> Your issues </h1>
	<@s.if test="!#user.validated">
		<p class="center bold">
		You have not defined any topics yet.<br/><br/>
		<a href="<@s.url namespace="/my-account" action="edit-profile" />">Click here</a> 
		to define topics to monitor news for -- you will also find some example topics to get you started.
		</p>
	</@s.if>
	<@s.else>
    <#assign issues=user.issues>
    <#if issues?exists>

		<#--### DISPLAY ISSUES ##### -->
		<div class="ie_center_hack">
    <div id="dl_div" style="padding:10px;">
    <span id="dl_hide" onclick="hideHelp();">Hide</span>
    <span id="dl_show" onclick="showHelp()">Help</span>
		<dl id="dl_help">
      <dt> Clear News: </dt> 
      <dd> Removes all filtered news from this issue. </dd>
      <dt> Freeze: </dt> 
      <dd> Once an issue is frozen, no new articles are downloaded
      into it.  But, old news in the issue continues to be available.  This is
      a good way to set up demo issues. </dd>
      <dt> Reclassify: </dt> 
      <dd>Classify from the archives.  This is useful when you have made changes 
      to your issue and want those changes reflected. 
      <strong> Please go easy on using this and use only if necessary since this
      is resource-intensive at this time! </strong>
      </dd>
		</dl>
    </div>

		<table cellspacing="0" class="userissuelisting">
		<tr class="tblhdr">
			<td>Issue</td>
			<td>Time of <br>last update</td>
		</tr>
      <#foreach i in issues>
		<tr class="issue_row">
			<td>
        <div>
          <#if i.frozen> (<span style="color:00a;font-weight:bold;"> FROZEN </span>) </#if>
          <a href="<@s.url namespace="/" action="browse" owner="${user.uid}" issue="${i.name}" />">${i.name}</a>
          [${i.numArticles}] &nbsp;
          <a class="rssfeed" href="${i.getRSSFeedURL()}"><img src="<@s.url namespace="/" value="/icons/rss-12x12.jpg"/>" alt="RSS 2.0"></a>
        </div>
        <div class="edit_bar">
          <a onclick="return confirmReset()" href="<@s.url namespace="/my-account" action="edit-profile"><@s.param name="action" value="'reset'" /><@s.param name="issue" value="'${i.name}'" /></@s.url>">Clear News</a>,
          <#if !i.frozen>
          <a href="<@s.url namespace="/my-account" action="edit-profile"><@s.param name="action" value="'freeze'" /><@s.param name="issue" value="'${i.name}'" /></@s.url>">Freeze</a>,
          <#else>
          <a style="color:#070;font-weight:bold;" href="<@s.url namespace="/my-account" action="edit-profile"><@s.param name="action" value="'unfreeze'" /><@s.param name="issue" value="'${i.name}'" /></@s.url>">Unfreeze</a>,
          </#if>
          <a href="<@s.url namespace="/forms" action="reclassify-news"><@s.param name="issue" value="'${i.name}'" /></@s.url>">Reclassify</a>
        </div>
			</td>
			<td class="center">
        <#assign lut=i.lastUpdateTime_String>
        <#if lut?exists> ${lut} <#else> -- </#if>
			</td>
		</tr>
			</#foreach>
		</table>
		</div>
		<#else>
      <p class="bold center">You have not validated your topics yet!</p>
      <p>Please go to the <a href="<@s.url namespace="/my-account" action="edit-profile" />">edit topics page</a> 
      and validate them to be able to monitor news</p>
		</#if> <#-- of #if issues -->
	</@s.else> <#-- of #if active-profile exists -->
	</td>
</tr>
</table>
</div>
</@s.if>
<@s.else> <#-- user signed in -->
<#include "/ftl/layout/no.user.ftl">
</@s.else>
<#include "/ftl/layout/footer.ftl" parse="n">
</body>
</html>
