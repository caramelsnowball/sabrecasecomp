import React from 'react';
import InfoPanel from './InfoPanel.js'
import '../css/sabre.css';
import '../App.css';

export default class Main extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      topic: "0",
      agent: undefined,
      average: undefined,
      float: "center"
    }
    this.topics = [
      "TICKETING/REFUNDS AND EXCHANGES",
      "PNR/BASIC ENTRY",
      "SYSTEM CONTROLS/EPR",
      "PNR/ITINERARY SEGMENT",
      "QUICK ASSIST/CALL BACK",
      "TICKETING/AUTO EXCHANGE-WFRF",
      "TICKETING/ERROR RESPONSE",
      "PRICING/FORMAT",
      "TICKETING/ELECTRONIC FAILCODE",
      "PNR/RETRIEVE",
      "TICKETING/OPTIONS"
    ]
  }

  handleChange(e) {
    this.setState({
      topic: e.target.value
    });
  }

  submitTopic(e) {
    e.preventDefault();
    fetch('http://localhost:8005/topic/' + this.state.topic)
      .then(res => res.json())
      .then(res => {
        this.setState({
        agent: res.agent,
        average: res.average,
        float: "left"
      })});
  }

  render() {
    return (
      <div>
        <form className="mainform" style={{float: this.state.float}}>
          <select className='select spark-select__input' value={this.state.topic} onChange={this.handleChange.bind(this)}>
            {Array.from([0,1,2,3,4,5,6,7,8,9]).map((i) => i.toString()).map((i) => {
              return <option value={i}>{this.topics[i]}</option>;
            })}
          </select>
          <button className='spark-btn spark-btn--md' onClick={this.submitTopic.bind(this)}>Submit</button>
        </form>
        {this.state.agent &&
        <div>
        <InfoPanel obj={this.state.agent} />
        <InfoPanel obj={this.state.average} />
        </div>
        }
      </div>
    );
  }
}
