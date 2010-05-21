<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>

<head>
<#include "/ftl/layout/common_includes.ftl">
<title>Rename file</title>
</head>

<body>

<@s.set name="user" value="#session.user" />
<@s.if test="#user">
<div class="bodymain">
<table class="userhome" cellspacing="0">
<#include "/ftl/layout/header.ftl">
<tr>
<#include "/ftl/layout/left.menu.ftl">
<td class="user_space">
<#include "/ftl/layout/errors.ftl">
				<!-- Password change form -->
		<h1> Renaming file ${Parameters.file}</h1>
		<div class="ie_center_hack">
		<form class="register" style="width:260px" action="<@s.url namespace="/file" action="rename" />" method="post">
		<input type="hidden" name="name" value="${Parameters.file}"> 
		<div class="formelt"> New Name : <input class="text" name="newname"> </div>
		<div align="center"> <input class="submit" name="submit" value="Rename" type="submit"> </div>
		</form>
		</div>
	</td>
</tr>
</table>
</div>
</@s.if>
<@s.else> <#include "/ftl/layout/no.user.ftl"> </@s.else>
<#include "/ftl/layout/footer.ftl" parse="n">
</body>
</html>
