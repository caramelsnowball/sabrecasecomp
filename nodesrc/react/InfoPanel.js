import React from 'react';

export default class InfoPanel extends React.Component {

  render() {
    var a = this.props.obj;
    return (
      <div className="agentData">
        <header className="spark-table__header">
          <h4 className="spark-table__title">
            {a.name}
          </h4>
        </header>
        <table className="spark-table">
          <tbody>
            <tr>
              <td>Average Time</td>
              <td>{a.avgTime}</td>
            </tr>
            <tr>
              <td>Number of Cases Resolved</td>
              <td>{a.numTaken}</td>
            </tr>
            <tr>
              <td>Success Rate</td>
              <td>{a.successRate}</td>
            </tr>
          </tbody>
        </table>
      </div>
    )
  }
}
