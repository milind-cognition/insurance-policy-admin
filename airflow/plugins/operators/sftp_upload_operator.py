"""Reusable SFTP upload operator wrapping paramiko.

Replaces the mainframe FTP steps in DAILY-EXTRACT.jcl (lines 86-96)
and MONTHLY-EXPOSURE.jcl (lines 63-69) with secure SFTP transfers.
"""

from __future__ import annotations

import logging
import os
from typing import Sequence

from airflow.hooks.base import BaseHook
from airflow.models import BaseOperator

import paramiko

log = logging.getLogger(__name__)


class SFTPUploadOperator(BaseOperator):
    """Upload one or more local files to a remote host via SFTP.

    Uses an Airflow connection for host/port/credentials (SSH key or
    password).  Falls back to SSH key authentication when the connection
    has a ``private_key`` extra field or a key file path.

    Parameters
    ----------
    sftp_conn_id:
        Airflow connection ID that stores host, port, login, and
        private-key or password credentials.
    local_paths:
        List of local file paths to upload.
    remote_paths:
        Corresponding list of remote destination paths.  Must be the
        same length as *local_paths*.
    """

    template_fields: Sequence[str] = ("local_paths", "remote_paths")

    def __init__(
        self,
        *,
        sftp_conn_id: str,
        local_paths: list[str],
        remote_paths: list[str],
        **kwargs,
    ) -> None:
        super().__init__(**kwargs)
        if len(local_paths) != len(remote_paths):
            raise ValueError(
                "local_paths and remote_paths must have the same length"
            )
        self.sftp_conn_id = sftp_conn_id
        self.local_paths = local_paths
        self.remote_paths = remote_paths

    def execute(self, context):
        conn = BaseHook.get_connection(self.sftp_conn_id)
        hostname = conn.host
        port = conn.port or 22
        username = conn.login

        transport = paramiko.Transport((hostname, port))
        try:
            extra = conn.extra_dejson or {}
            key_file = extra.get("key_file") or extra.get("private_key_file")
            if key_file:
                pkey = paramiko.RSAKey.from_private_key_file(
                    os.path.expanduser(key_file)
                )
                transport.connect(username=username, pkey=pkey)
            else:
                transport.connect(username=username, password=conn.password)

            sftp = paramiko.SFTPClient.from_transport(transport)
            try:
                for local, remote in zip(self.local_paths, self.remote_paths):
                    log.info("Uploading %s -> %s:%s", local, hostname, remote)
                    sftp.put(local, remote)
                    log.info("Upload complete: %s", remote)
            finally:
                sftp.close()
        finally:
            transport.close()
